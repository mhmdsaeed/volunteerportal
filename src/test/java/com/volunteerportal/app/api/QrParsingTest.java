package com.volunteerportal.app.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QrParsingTest {

    @Test
    void checkInLink_givesEventIdAndCode_whateverTheHost() {
        assertThat(MobileApiService.parseQr("https://192.168.100.11:8443/checkin/112?code=59672954-8LotNIr81yveL5_c8ToBUw"))
                .contains(new MobileApiService.ScannedCode(112L, "59672954-8LotNIr81yveL5_c8ToBUw"));
        assertThat(MobileApiService.parseQr("  http://portal.example.org/checkin/7/?code=1-abc&lang=ar "))
                .contains(new MobileApiService.ScannedCode(7L, "1-abc"));
    }

    @Test
    void otherQrCodes_areNotCheckInCodes() {
        assertThat(MobileApiService.parseQr("https://example.com/checkin/112")).isEmpty(); // no code
        assertThat(MobileApiService.parseQr("https://example.com/checkin/abc?code=1-x")).isEmpty();
        assertThat(MobileApiService.parseQr("https://example.com/admin/112?code=1-x")).isEmpty();
        assertThat(MobileApiService.parseQr("https://example.com/checkin/112/extra?code=1-x")).isEmpty();
        assertThat(MobileApiService.parseQr("WIFI:S:office;T:WPA;P:secret;;")).isEmpty();
        assertThat(MobileApiService.parseQr("")).isEmpty();
        assertThat(MobileApiService.parseQr(null)).isEmpty();
    }
}
