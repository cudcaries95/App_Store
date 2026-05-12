package com.example.app_store.Model;

import java.io.Serializable;

public class ShippingAddress implements Serializable {
    private String id;
    private String receiverName;
    private String phone;
    private String detailedAddress;
    private boolean isDefault;

    public ShippingAddress() {
    }

    public ShippingAddress(String id, String receiverName, String phone, String detailedAddress, boolean isDefault) {
        this.id = id;
        this.receiverName = receiverName;
        this.phone = phone;
        this.detailedAddress = detailedAddress;
        this.isDefault = isDefault;
    }

    public ShippingAddress(String receiverName, String phone, String detailedAddress, boolean isDefault) {
        this.receiverName = receiverName;
        this.phone = phone;
        this.detailedAddress = detailedAddress;
        this.isDefault = isDefault;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDetailedAddress() {
        return detailedAddress;
    }

    public void setDetailedAddress(String detailedAddress) {
        this.detailedAddress = detailedAddress;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
}
