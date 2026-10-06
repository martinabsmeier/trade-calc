/*
 * Copyright 2026 Martin Absmeier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.am.albion.tradecalc.service;

import de.am.albion.tradecalc.dataprovider.albion.AlbionDataApiClient;
import de.am.albion.tradecalc.dataprovider.albion.dto.MarketJson;
import de.am.albion.tradecalc.dataprovider.albion.mapper.MarketJsonMapper;
import de.am.albion.tradecalc.domain.model.MarketPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PriceService}. Exercises the mapping contract between
 * the upstream DTO and the domain record and pins down the single-shot client
 * call used to fetch prices for one item.
 */
class PriceServiceTest {

    private AlbionDataApiClient client;
    private MarketJsonMapper mapper;
    private PriceService service;

    /**
     * Builds a service with a mock client and a real mapper (mapper is pure
     * and needs no IO).
     */
    @BeforeEach
    void setUp() {
        client = mock(AlbionDataApiClient.class);
        mapper = new MarketJsonMapper();
        service = new PriceService(client, mapper);
    }

    /**
     * A single client call returns multiple cities; the returned map has one
     * entry per city and the prices are normalised (no zero sentinels).
     */
    @Test
    void getPrices_returnsOneEntryPerCity() {
        MarketJson woodLym = new MarketJson("T4_WOOD", "Lymhurst", 1,
                new BigDecimal("107"), null, Instant.parse("2026-10-05T04:40:00Z"), null);
        MarketJson woodFort = new MarketJson("T4_WOOD", "Fort Sterling", 1,
                new BigDecimal("124"), null, Instant.parse("2026-10-05T05:20:00Z"), null);
        when(client.fetchPrices("T4_WOOD")).thenReturn(List.of(woodLym, woodFort));

        Map<String, MarketPrice> prices = service.getPrices("T4_WOOD");

        assertThat(prices).containsOnlyKeys("Lymhurst", "Fort Sterling");
        assertThat(prices.get("Lymhurst").sellPriceMin()).isEqualByComparingTo("107");
        assertThat(prices.get("Fort Sterling").sellPriceMin()).isEqualByComparingTo("124");
    }

    /**
     * A buy-price of {@code 0} (no active buy orders) is normalised to
     * {@code null} by the service.
     */
    @Test
    void getPrices_zeroBuyPrice_normalisesToNull() {
        MarketJson bow = new MarketJson("T4_BOW", "Lymhurst", 1,
                BigDecimal.ZERO, BigDecimal.ZERO, Instant.EPOCH, Instant.EPOCH);
        when(client.fetchPrices("T4_BOW")).thenReturn(List.of(bow));

        MarketPrice result = service.getPrices("T4_BOW").get("Lymhurst");

        assertThat(result.buyPriceMax()).isNull();
        assertThat(result.sellPriceMin()).isNull();
    }

    /**
     * Entries whose {@code item_id} does not match the requested item are
     * silently dropped — defends against malformed upstream responses.
     */
    @Test
    void getPrices_dropsMismatchedItems() {
        MarketJson wood = new MarketJson("T4_WOOD", "Lymhurst", 1,
                new BigDecimal("100"), null, Instant.now(), null);
        MarketJson bow = new MarketJson("T4_BOW", "Lymhurst", 1,
                new BigDecimal("9999"), null, Instant.now(), null);
        when(client.fetchPrices("T4_WOOD")).thenReturn(List.of(wood, bow));

        Map<String, MarketPrice> prices = service.getPrices("T4_WOOD");

        assertThat(prices.get("Lymhurst").sellPriceMin()).isEqualByComparingTo("100");
    }

    /**
     * Blank or {@code null} item ids short-circuit to an empty map without
     * hitting the upstream API.
     */
    @Test
    void getPrices_blankItemId_returnsEmptyWithoutClientCall() {
        assertThat(service.getPrices(null)).isEmpty();
        assertThat(service.getPrices("")).isEmpty();
        assertThat(service.getPrices("   ")).isEmpty();
        verifyNoMoreInteractions(client);
    }

    /**
     * The returned map is immutable, so callers cannot accidentally mutate
     * the cached value.
     */
    @Test
    void getPrices_returnsImmutableMap() {
        when(client.fetchPrices("T4_WOOD")).thenReturn(List.of());
        Map<String, MarketPrice> prices = service.getPrices("T4_WOOD");

        assertThat(prices).isEmpty();
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> prices.put("Lymhurst", null));
    }

    /**
     * Verifies the exact argument shape sent to the upstream client for a
     * single-item request — guards against accidental varargs changes.
     */
    @Test
    void getPrices_passesItemIdAsVarargsToClient() {
        when(client.fetchPrices(any(String.class))).thenReturn(List.of());

        service.getPrices("T4_BOW");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(client, times(1)).fetchPrices(captor.capture());
        assertThat(captor.getAllValues()).containsExactly("T4_BOW");
    }
}