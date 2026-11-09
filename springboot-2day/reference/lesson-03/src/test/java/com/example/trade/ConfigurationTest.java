package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.trade.config.TradeProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Shows that profiles change configuration: same code, different values.
class ConfigurationTest {

    @Nested
    @SpringBootTest(properties = "spring.main.web-application-type=none")
    class DefaultProfile {
        @Autowired TradeProperties props;

        @Test
        void usesValuesFromTheBaseFile() {
            assertThat(props.maxQuantity()).isEqualByComparingTo("1000000");
            assertThat(props.venue()).isEqualTo("XLON");
        }
    }

    @Nested
    @SpringBootTest(properties = "spring.main.web-application-type=none")
    @ActiveProfiles("prod")
    class ProdProfile {
        @Autowired TradeProperties props;

        @Test
        void prodFileOverridesTheBaseFile() {
            assertThat(props.maxQuantity()).isEqualByComparingTo("500000");
            assertThat(props.venue()).isEqualTo("XLON");   // not overridden, so the base value remains
        }
    }

    @Nested
    @SpringBootTest(properties = {"spring.main.web-application-type=none", "trade.max-quantity=500"})
    class ExplicitOverride {
        @Autowired TradeProperties props;

        @Test
        void aPropertyOnTheTestOrCommandLineBeatsTheFiles() {
            assertThat(props.maxQuantity()).isEqualByComparingTo("500");
        }
    }
}
