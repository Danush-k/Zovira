package com.zovira.delivery.service;

import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.delivery.dto.DeliveryEstimate;
import com.zovira.order.entity.DeliveryOption;
import com.zovira.settings.dto.PlatformSettings;
import com.zovira.settings.service.SettingsService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Serviceability and delivery-date estimation by PIN code.
 *
 * <p>Rules: metro PIN prefixes deliver a day faster and are eligible for express; the
 * north-east, Jammu and Kashmir and Ladakh take two extra days; island territories four extra and
 * no express. Army Postal Service PINs (starting with 9) are not serviceable. Deliveries are not
 * made on Sundays. Base timings and fees come from platform settings.
 */
@Service
public class DeliveryService {

    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private static final Set<String> METRO_PREFIXES = Set.of(
            "110", "400", "560", "600", "700", "500", "411", "380", "122", "201");
    private static final Set<String> REMOTE_PREFIXES = Set.of("78", "79", "18", "19");
    /** Andaman and Nicobar Islands. */
    private static final String ISLAND_PREFIX = "744";

    private final SettingsService settingsService;
    private final Clock clock;

    public DeliveryService(SettingsService settingsService, Clock clock) {
        this.settingsService = settingsService;
        this.clock = clock;
    }

    public DeliveryEstimate estimate(String pincode) {
        if (pincode == null || !pincode.matches("^[1-9]\\d{5}$")) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "Enter a valid 6-digit PIN code");
        }
        PlatformSettings s = settingsService.current();
        if (pincode.startsWith("9")) {
            return new DeliveryEstimate(pincode, false, "Not serviceable", null, null, false);
        }
        boolean metro = METRO_PREFIXES.contains(pincode.substring(0, 3));
        boolean island = pincode.startsWith(ISLAND_PREFIX);
        boolean remote = REMOTE_PREFIXES.contains(pincode.substring(0, 2));

        int standardDays = Math.max(1, s.standardDeliveryDays() + (metro ? -1 : 0) + (remote ? 2 : 0) + (island ? 4 : 0));
        LocalDate today = LocalDate.now(clock.withZone(IST));
        DeliveryEstimate.Option standard = new DeliveryEstimate.Option(true, addDeliveryDays(today, standardDays),
                standardDays, s.standardShippingFee(), s.freeShippingThreshold());
        DeliveryEstimate.Option express = metro
                ? new DeliveryEstimate.Option(true, addDeliveryDays(today, s.expressDeliveryDays()),
                        s.expressDeliveryDays(), s.expressShippingFee(), null)
                : new DeliveryEstimate.Option(false, null, 0, s.expressShippingFee(), null);
        String zone = metro ? "Metro" : island ? "Island territory" : remote ? "Remote area" : "Standard";
        return new DeliveryEstimate(pincode, true, zone, standard, express, s.codEnabled() && !island);
    }

    public LocalDate estimatedDate(String pincode, DeliveryOption option) {
        DeliveryEstimate estimate = estimate(pincode);
        if (!estimate.serviceable()) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "We don't deliver to PIN code " + pincode + " yet");
        }
        if (option == DeliveryOption.EXPRESS) {
            if (!estimate.express().available()) {
                throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
                        "Express delivery isn't available for PIN code " + pincode);
            }
            return estimate.express().date();
        }
        return estimate.standard().date();
    }

    static LocalDate addDeliveryDays(LocalDate from, int days) {
        LocalDate date = from;
        int added = 0;
        while (added < days) {
            date = date.plusDays(1);
            if (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return date;
    }
}
