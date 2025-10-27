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

import com.bytechef.component.definition.unified.crm.model.ProviderContactInputModel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
// CHECKSTYLE:OFF
public class HubspotContactInputModel extends HashMap<String, Object> implements ProviderContactInputModel {

    private String firstname;
    private String lastname;
    private String email;
    private String phone;
    private String city;
    private String country;
    private String zip;
    private String state;
    private String addres;
    private String mobilephone;
    private String hubspot_owner_id;
    private String associatedcompanyid;
    private String fax;
    private String jobtitle;

    protected HubspotContactInputModel() {
    }

    public HubspotContactInputModel(
        String firstname, String lastname, String email, String phone, String city, String country, String zip,
        String state, String addres, String mobilephone, String hubspot_owner_id, String associatedcompanyid,
        String fax, String jobtitle, Map<String, Object> customFields) {

        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.phone = phone;
        this.city = city;
        this.country = country;
        this.zip = zip;
        this.state = state;
        this.addres = addres;
        this.mobilephone = mobilephone;
        this.hubspot_owner_id = hubspot_owner_id;
        this.associatedcompanyid = associatedcompanyid;
        this.fax = fax;
        this.jobtitle = jobtitle;

        this.putAll(customFields);
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getZip() {
        return zip;
    }

    public String getState() {
        return state;
    }

    public String getAddres() {
        return addres;
    }

    public String getMobilephone() {
        return mobilephone;
    }

    public String getHubspot_owner_id() {
        return hubspot_owner_id;
    }

    public String getAssociatedcompanyid() {
        return associatedcompanyid;
    }

    public String getFax() {
        return fax;
    }

    public String getJobtitle() {
        return jobtitle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HubspotContactInputModel that)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        return Objects.equals(firstname, that.firstname) && Objects.equals(lastname, that.lastname) &&
            Objects.equals(email, that.email) && Objects.equals(phone, that.phone) &&
            Objects.equals(city, that.city) && Objects.equals(country, that.country) &&
            Objects.equals(zip, that.zip) && Objects.equals(state, that.state) &&
            Objects.equals(addres, that.addres) && Objects.equals(mobilephone, that.mobilephone) &&
            Objects.equals(hubspot_owner_id, that.hubspot_owner_id) &&
            Objects.equals(associatedcompanyid, that.associatedcompanyid) && Objects.equals(fax, that.fax) &&
            Objects.equals(jobtitle, that.jobtitle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            super.hashCode(), firstname, lastname, email, phone, city, country, zip, state, addres, mobilephone,
            hubspot_owner_id, associatedcompanyid, fax, jobtitle);
    }

    @Override
    public String toString() {
        return "HubspotContactInputModel{" +
            "firstname='" + firstname + '\'' +
            ", lastname='" + lastname + '\'' +
            ", email='" + email + '\'' +
            ", phone='" + phone + '\'' +
            ", city='" + city + '\'' +
            ", country='" + country + '\'' +
            ", zip='" + zip + '\'' +
            ", state='" + state + '\'' +
            ", addres='" + addres + '\'' +
            ", mobilephone='" + mobilephone + '\'' +
            ", hubspot_owner_id='" + hubspot_owner_id + '\'' +
            ", associatedcompanyid='" + associatedcompanyid + '\'' +
            ", fax='" + fax + '\'' +
            ", jobtitle='" + jobtitle + '\'' +
            "} " + super.toString();
    }
}
