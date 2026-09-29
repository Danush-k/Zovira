package com.zovira.common.validation;

public final class ValidationPatterns {

    /** Indian mobile number: 10 digits starting 6-9. */
    public static final String INDIAN_MOBILE = "^[6-9]\\d{9}$";

    /** Indian postal PIN code: 6 digits, first digit non-zero. */
    public static final String PINCODE = "^[1-9]\\d{5}$";

    /** GSTIN: 2-digit state code, PAN, entity number, 'Z', checksum. */
    public static final String GSTIN = "^\\d{2}[A-Z]{5}\\d{4}[A-Z][1-9A-Z]Z[0-9A-Z]$";

    private ValidationPatterns() {
    }
}
