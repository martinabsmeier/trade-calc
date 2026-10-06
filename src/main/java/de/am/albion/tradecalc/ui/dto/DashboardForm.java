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
package de.am.albion.tradecalc.ui.dto;

import de.am.albion.tradecalc.domain.CalculationMode;

/**
 * Form backing-bean for the dashboard. Kept tiny: the only inputs the user
 * makes on the landing page are city, mode, optional {@code top} and an
 * optional "include crafting plan" flag.
 */
public class DashboardForm {

    private String homeCity = "Lymhurst";
    private CalculationMode mode = CalculationMode.BEST_OF_ALL;
    private int top = 20;
    private boolean includePlan;

    public String getHomeCity() {
        return homeCity;
    }

    public void setHomeCity(String homeCity) {
        this.homeCity = homeCity;
    }

    public CalculationMode getMode() {
        return mode;
    }

    public void setMode(CalculationMode mode) {
        this.mode = mode;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public boolean isIncludePlan() {
        return includePlan;
    }

    public void setIncludePlan(boolean includePlan) {
        this.includePlan = includePlan;
    }
}