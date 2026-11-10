package com.pharmacy.drugstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pharmacy.drugstore.shipment.ShipmentStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "shipments")
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String trackingNumber;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false, length = 6)
    private String pincode;

    @Column(nullable = false, length = 15)
    private String contactNumber;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    private ShipmentStatus status = ShipmentStatus.PENDING;

    private String carrier = "Medicine Express";
    private Instant estimatedDelivery;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Instant shippedAt;
    private Instant deliveredAt;

    @JsonIgnore
    @OneToOne(optional = false)
    @JoinColumn(name = "order_id", unique = true)
    private CustomerOrder order;

    public Long getId() { return id; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }
    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }
    public Instant getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(Instant estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getShippedAt() { return shippedAt; }
    public void setShippedAt(Instant shippedAt) { this.shippedAt = shippedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    @JsonIgnore
    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }

    public Long getOrderId() { return order == null ? null : order.getId(); }
    public String getOrderNumber() { return order == null ? null : order.getOrderNumber(); }

    public String getFullAddress() {
        return String.join(", ",
                blankToDash(address),
                blankToDash(city),
                blankToDash(state),
                blankToDash(pincode));
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
