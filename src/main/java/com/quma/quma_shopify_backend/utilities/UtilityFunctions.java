package com.quma.quma_shopify_backend.utilities;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class UtilityFunctions {

    public static String getRandomId() {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String random = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return today + "-" + random;
    }
}
