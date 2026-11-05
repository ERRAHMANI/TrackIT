package com.trackit.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.trackit.config.TrackItProperties;
import com.trackit.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Accès à l'API Stripe (Checkout). Les clés proviennent exclusivement de l'environnement. */
@Component
public class StripeGateway {

    private final TrackItProperties.Stripe config;
    private final String frontendUrl;

    public StripeGateway(TrackItProperties properties) {
        this.config = properties.stripe();
        this.frontendUrl = properties.frontendUrl();
    }

    public record SessionPaiement(String id, String url) {
    }

    public SessionPaiement creerSession(Long idAbonnement, String libelleOffre, BigDecimal montant, String emailClient) {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/index.html?paiement=retour&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/index.html?paiement=annule")
                .setClientReferenceId(String.valueOf(idAbonnement))
                .setCustomerEmail(emailClient)
                .putMetadata("id_abonnement", String.valueOf(idAbonnement))
                .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                        .putMetadata("id_abonnement", String.valueOf(idAbonnement)).build())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(config.currency())
                                .setUnitAmount(montant.movePointRight(2).longValueExact())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("TrackIT — " + libelleOffre).build())
                                .build())
                        .build())
                .build();
        try {
            Session session = Session.create(params, options());
            return new SessionPaiement(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw erreurStripe(e);
        }
    }

    public Session recupererSession(String idSession) {
        try {
            return Session.retrieve(idSession, options());
        } catch (StripeException e) {
            throw erreurStripe(e);
        }
    }

    public void expirerSession(String idSession) {
        try {
            Session session = Session.retrieve(idSession, options());
            if ("open".equals(session.getStatus())) {
                session.expire(options());
            }
        } catch (StripeException e) {
            throw erreurStripe(e);
        }
    }

    public Event verifierEvenement(String charge, String signature) {
        if (config.webhookSecret() == null || config.webhookSecret().isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "STRIPE_WEBHOOK_SECRET n'est pas configuré.");
        }
        try {
            return Webhook.constructEvent(charge, signature, config.webhookSecret());
        } catch (SignatureVerificationException e) {
            throw ApiException.invalide("Signature Stripe invalide.");
        }
    }

    private RequestOptions options() {
        return RequestOptions.builder().setApiKey(config.secretKey()).build();
    }

    private static ApiException erreurStripe(StripeException e) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "Erreur Stripe : " + e.getMessage());
    }
}
