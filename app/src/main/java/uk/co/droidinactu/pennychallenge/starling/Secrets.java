package uk.co.droidinactu.pennychallenge.starling;

import lombok.Data;

@Data
public class Secrets {

    private String clientId;
    private String clientSecret;
    private String publicKeyUid;
    private String cookieSecret;
    private String personalTagStore;
    private String productionApi;
    private String personalAccessToken;

}
