package com.projects.airbnb.controller;

import com.projects.airbnb.service.BookingService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final BookingService bookingService;

    @Value("${stripe.webhook.secret}")
    private String endPointSecret;

    @PostMapping(path = "/payment")
    public ResponseEntity<Void> capturePayments(@RequestBody String payLoad,
                                                @RequestHeader("Stripe-Signature")
                                                String signHeader) {
        try {
            Event event = Webhook.constructEvent(payLoad, signHeader, endPointSecret);
            bookingService.capturePayment(event);
            return ResponseEntity.noContent().build();
        } catch (SignatureVerificationException e) {
            throw new RuntimeException(e);
        }
    }
}
