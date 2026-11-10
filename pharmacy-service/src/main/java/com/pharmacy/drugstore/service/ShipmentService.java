package com.pharmacy.drugstore.service;

import com.pharmacy.drugstore.dto.PaymentRequest;
import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.Shipment;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.logging.RequestMdc;
import com.pharmacy.drugstore.repository.ShipmentRepository;
import com.pharmacy.drugstore.shipment.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ShipmentService {
    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);
    private static final Pattern EMAIL = Pattern.compile("^[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final ShipmentRepository shipments;

    public ShipmentService(ShipmentRepository shipments) {
        this.shipments = shipments;
    }

    public void requireDelivery(PaymentRequest request, User user) {
        validate(request, user);
    }

    public Shipment attach(CustomerOrder order, User user, PaymentRequest request) {
        validate(request, user);
        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(nextTrackingNumber());
        shipment.setRecipientName(clean(request.recipientName()));
        shipment.setAddress(clean(request.address()));
        shipment.setCity(clean(request.city()));
        shipment.setState(clean(request.state()));
        shipment.setPincode(digits(request.pincode()));
        shipment.setContactNumber(digits(request.contactNumber()));
        shipment.setEmail(resolveEmail(request, user));
        shipment.setCarrier("Medicine Express");
        shipment.setEstimatedDelivery(Instant.now().plus(2, ChronoUnit.DAYS));
        shipment.setOrder(order);
        shipment.setStatus(ShipmentStatus.PENDING);
        order.setShipment(shipment);
        shipments.saveAndFlush(shipment);
        log.info("Shipment prepared {} tracking={} orderNumber={}",
                RequestMdc.describe(user), shipment.getTrackingNumber(), order.getOrderNumber());
        return shipment;
    }

    public void markPaymentResult(Shipment shipment, boolean paid) {
        if (shipment == null) {
            return;
        }
        Instant now = Instant.now();
        if (paid) {
            shipment.setStatus(ShipmentStatus.CONFIRMED);
        } else {
            shipment.setStatus(ShipmentStatus.CANCELLED);
        }
        shipment.setUpdatedAt(now);
        shipments.saveAndFlush(shipment);
    }

    @Transactional(readOnly = true)
    public List<Shipment> list(User user) {
        List<Shipment> list = shipments.findByUserIdOrderByCreatedAtDesc(user.getId());
        log.info("Shipment list {} count={}", RequestMdc.describe(user), list.size());
        return list;
    }

    @Transactional(readOnly = true)
    public Shipment getByTracking(User user, String trackingNumber) {
        return shipments.findByTrackingNumberAndUserId(trackingNumber, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));
    }

    @Transactional(readOnly = true)
    public Shipment getByOrder(User user, String orderNumber) {
        return shipments.findByOrderNumberAndUserId(orderNumber, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));
    }

    private void validate(PaymentRequest request, User user) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery details are required");
        }
        require(request.recipientName(), "Enter the recipient name");
        require(request.address(), "Enter the delivery address");
        require(request.city(), "Enter city");
        require(request.state(), "Enter state");
        String pin = digits(request.pincode());
        if (pin.length() != 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a 6-digit pincode");
        }
        String phone = digits(request.contactNumber());
        if (phone.length() != 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a 10-digit contact number");
        }
        if (!EMAIL.matcher(resolveEmail(request, user)).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email ID");
        }
    }

    private static String resolveEmail(PaymentRequest request, User user) {
        String email = clean(request == null ? null : request.email());
        if (email.isBlank() && user != null) {
            email = clean(user.getEmail());
        }
        return email.toLowerCase(Locale.ROOT);
    }

    private static void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String digits(String value) {
        return clean(value).replaceAll("\\D", "");
    }

    private String nextTrackingNumber() {
        return "SHP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
