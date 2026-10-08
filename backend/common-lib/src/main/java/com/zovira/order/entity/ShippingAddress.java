package com.zovira.order.entity;

import com.zovira.user.entity.Address;

/** Immutable snapshot of the delivery address captured when the order is placed. */
public class ShippingAddress {

    private String name;
    private String phone;
    private String line1;
    private String line2;
    private String landmark;
    private String city;
    private String state;
    private String pincode;
    private String country;

    public ShippingAddress() {
    }

    public static ShippingAddress from(Address a) {
        ShippingAddress s = new ShippingAddress();
        s.name = a.getFullName();
        s.phone = a.getPhone();
        s.line1 = a.getLine1();
        s.line2 = a.getLine2();
        s.landmark = a.getLandmark();
        s.city = a.getCity();
        s.state = a.getState();
        s.pincode = a.getPincode();
        s.country = a.getCountry();
        return s;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getLandmark() {
        return landmark;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
