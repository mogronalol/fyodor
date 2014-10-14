package uk.org.fyodor.generators;

import java.util.Locale;
import java.util.MissingResourceException;

public class Iso3CountryGenerator implements Generator<String> {

    Generator<Locale> localeGenerator = RDG.locales();

    public String next() {
        String country;
        try {
            country = localeGenerator.next().getISO3Country();
        } catch (MissingResourceException e) {
            return next();
        }
        if (country.isEmpty()) {
            return next();
        }
        return country;
    }
}
