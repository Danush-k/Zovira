package com.zovira.order.entity;

import com.zovira.user.entity.Address;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Immutable snapshot of the delivery address captured when the order is placed. */
@Embeddable
public class ShippingAddress {

    @Column(name = "ship_name", nullable = false, length = 120)
    private String name;

    @Column(name = "ship_phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "ship_line1", nullable = false, length = 200)
    private String line1;

    @Column(name = "ship_line2", length = 200)
    private String line2;

    @Column(name = "ship_landmark", length = 120)
    private String landmark;

    @Column(name = "ship_city", nullable = false, length = 80)
    private String city;

    @Column(name = "ship_state", nullable = false, length = 80)
    private String state;

    @Column(name = "ship_pincode", nullable = false, length = 10)
    private String pincode;

    @Column(name = "ship_country", nullable = false, length = 2)
    private String country;

    protected ShippingAddress() {
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

    public String getPhone() {
        return phone;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getLandmark() {
        return landmark;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPincode() {
        return pincode;
    }

    public String getCountry() {
        return country;
    }
}
