package com.trufflear.search.config

import org.jose4j.jwk.JsonWebKeySet

internal val jsonWebKeySet = JsonWebKeySet(
    "{\"keys\":[{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"qyJcsVJAF5zoGy+wX8bqrRr6Knd1NNgDZUSTVWon8eo=\",\"kty\":\"RSA\",\"n\":\"1ipt" +
            "8cgpD80sIJGYGYfRq1qCfCovAATy6G7iTeXnD0zXyBWCxeZtu36YWzEW_vaumzYxwkwccTx9G-wpRiON23dy6KwxH3ZOw5eipe0YMtrsu4OBwAJlrWiCiUYIajUapKB4dz" +
            "EjOI51sytzv_69SzlA00i8sto99sOkzyi0O8YgUXLxwnmDH8PFf_Q2aFhJi9PwIUKNIIFGIPkxcKAEPqpytSfKOTha_v2YHLrEvtFF5SCUJUoNcyzL2EI0i08GATxOPd" +
            "YJJMvuIctEp-yCZxDzR0CnjXJEEMqblZCl1Qn1f3PukrzEeADzk21wwaJRohT3R5RtarrTohLl4tpo-w\",\"use\":\"sig\"},{\"alg\":\"RS256\",\"e\":\"A" +
            "QAB\",\"kid\":\"L4oLY7pjCRtn8Dts6JvuQhZCoqJQZiHuXpCO59oxBlM=\",\"kty\":\"RSA\",\"n\":\"wyXJycxHIzzUfl5-sGiOhXw8SfNVUGYTvMft2T0zAsq" +
            "YHZvgx-Vs8Pw819ucAEnmZM_gqkJWlrVdbigL2eEymNaotOVJXfbPc64yKcifS95dBH6GcysYvadTSkDb38FHSWsYBO5kmVoEaKfnmDK2VevbK6KV6Eikf6bSytFU0fQnC" +
            "BJpFkfXwG0bOsOk_sAB2VYNaNUmC85ewFkkAqK_UkY7HPwtO4d0stEQRc-bz_tckxClM26O72-JdSkvP2JMD0XeF4SHpeZm8i6I98q8Rh2uSbmi_pAD149mG4pSJho0W_p" +
            "qCkW_MRd7BJqd4Uk-cShrQOiH1-VDQ2Aw-_JORQ\",\"use\":\"sig\"}]}"
)

internal val audience = "9hfqrqke1n3c4ogrij735lf5i"

internal val issuer = "https://cognito-idp.us-west-1.amazonaws.com/us-west-1_kSahivpup"