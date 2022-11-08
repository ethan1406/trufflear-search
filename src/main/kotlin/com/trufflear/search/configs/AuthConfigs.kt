package com.trufflear.search.configs

import org.jose4j.jwk.JsonWebKeySet

internal val jsonWebKeySet = JsonWebKeySet(
    "{\"keys\":[{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"wGWw5TUEWLEogkgRMBNpa8vyKV5yeiZOhzMdIWQMuRE=\",\"kty\":\"RSA\",\"n\":\"7dR9ObkQQijb2xXbRzNx" +
            "J35D3g3qd1muOP6LG_2HAqmNWVsA6B77gIPh1FDqdmUmFYvL6mtPKH90J7yb_EeVBHzpzt0_TczUX8pB7bxcaU5QCqNMA1kndbzPfQmI2ZTMuZBhHlHhvIYb7DqNzTqjgwLUBz" +
            "enWirdb0yLPxogUIH_RbkbDA49lgzqW6-7qz2zlV-0iREfTGr0y3U1hYDeGU5kJ9iaqvhulTatRtzuku4JKBL8Z7PE0NCsW1MpQUYlqTpFS64Rz-DXCVpV4HFzlKCJwp_EKeoC" +
            "uVEx3PkcM9W7DtqokN9eWMkOS2eVwJjcXkA1J3OsMSPkOGbhZVmp2w\",\"use\":\"sig\"},{\"alg\":\"RS256\",\"e\":\"AQAB\",\"kid\":\"SrmuUVeXnPzn78PFiN2" +
            "uaJ3HghFNYdxSvtBsr3L2xuM=\",\"kty\":\"RSA\",\"n\":\"vcIBTimQ-pcaHzQOFZpQbbGx4GZ5pFVCplNlxX6w-GHFGHSHaVwOs5aHGgmhXjpGIFx4bG3G6AW89G8z4kw" +
            "Db6p1lffqADIlSp_EeoehhWG7K2B2hOigyLM1-CIw0jB97EzZqHaD9ySdt77rQmwOLAV6dzak4vStfO3DedFXqDw37rDLart5Y5pgJGkn2y2bnFPlP1rqJ9KWN8KCRUcO42M_i" +
            "ydQAQ68jbvzXHd_S9gAuO8sVcSBow7tI_CzHfOXsUANoqHcO01NDti67PztqJN4XFq2S0cZ0-e0_zDmYa0U0uxtyxs7UvGAEcFVA-QNHqFBICWLUe9POtLldPPrnQ\",\"use\":\"sig\"}]}"
)

internal val audience = "2aup1dvq1ka522ijtemra2o8sq"

internal val issuer = "https://cognito-idp.us-west-1.amazonaws.com/us-west-1_4EQ8Ds6Jc"