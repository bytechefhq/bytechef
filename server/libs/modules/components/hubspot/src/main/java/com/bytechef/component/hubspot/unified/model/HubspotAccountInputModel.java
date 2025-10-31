/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.hubspot.unified.model;

import com.bytechef.component.definition.unified.crm.model.ProviderAccountInputModel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
// CHECKSTYLE:OFF
public class HubspotAccountInputModel extends HashMap<String, Object> implements ProviderAccountInputModel {

    private String city;
    private String name;
    private String phone;
    private String domain;
    private String industry;
    private String state;
    private String country;
    private String address;
    private String numberofemployees;
    private String zip;
    private String hubspot_owner_id;

    protected HubspotAccountInputModel() {
    }

    public HubspotAccountInputModel(
        String city, String name, String phone, String domain, String industry, String state, String country,
        String address, String numberofemployees, String zip, String hubspot_owner_id,
        Map<String, Object> customFields) {

        this.city = city;
        this.name = name;
        this.phone = phone;
        this.domain = domain;
        this.industry = industry;
        this.state = state;
        this.country = country;
        this.address = address;
        this.numberofemployees = numberofemployees;
        this.zip = zip;
        this.hubspot_owner_id = hubspot_owner_id;

        this.putAll(customFields);
    }

    public String getCity() {
        return city;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getDomain() {
        return domain;
    }

    public String getIndustry() {
        return industry;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }

    public String getAddress() {
        return address;
    }

    public String getnumberofemployees() {
        return numberofemployees;
    }

    public String getZip() {
        return zip;
    }

    public String getHubspot_owner_id() {
        return hubspot_owner_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof HubspotAccountInputModel that)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        return Objects.equals(city, that.city) && Objects.equals(name, that.name) &&
            Objects.equals(phone, that.phone) && Objects.equals(domain, that.domain) &&
            Objects.equals(industry, that.industry) && Objects.equals(state, that.state) &&
            Objects.equals(country, that.country) && Objects.equals(address, that.address) &&
            Objects.equals(numberofemployees, that.numberofemployees) && Objects.equals(zip, that.zip) &&
            Objects.equals(hubspot_owner_id, that.hubspot_owner_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            super.hashCode(), city, name, phone, domain, industry, state, country, address, numberofemployees, zip,
            hubspot_owner_id);
    }

    @Override
    public String toString() {
        return "HubspotAccountInputModel{" +
            "city='" + city + '\'' +
            ", name='" + name + '\'' +
            ", phone='" + phone + '\'' +
            ", domain='" + domain + '\'' +
            ", industry='" + industry + '\'' +
            ", state='" + state + '\'' +
            ", country='" + country + '\'' +
            ", address='" + address + '\'' +
            ", numberOfEmployees='" + numberofemployees + '\'' +
            ", zip='" + zip + '\'' +
            ", hubspotOwnerId='" + hubspot_owner_id + '\'' +
            "} " + super.toString();
    }
}
