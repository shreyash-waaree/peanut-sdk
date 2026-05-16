package org.eclipse.californium.core.observe;

import org.eclipse.californium.core.coap.Request;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/observe/ObservationUtil.class */
public final class ObservationUtil {
    public static Observation shallowClone(Observation observation) {
        if (null == observation) {
            return null;
        }
        Request request = observation.getRequest();
        if (null == request) {
            throw new IllegalArgumentException("missing request for observation!");
        }
        Request clonedRequest = new Request(request.getCode());
        clonedRequest.setDestinationContext(request.getDestinationContext());
        clonedRequest.setType(request.getType());
        clonedRequest.setMID(request.getMID());
        clonedRequest.setToken(request.getToken());
        clonedRequest.setOptions(request.getOptions());
        if (request.isUnintendedPayload()) {
            clonedRequest.setUnintendedPayload();
        }
        clonedRequest.setPayload(request.getPayload());
        clonedRequest.setUserContext(request.getUserContext());
        clonedRequest.setMaxResourceBodySize(request.getMaxResourceBodySize());
        return new Observation(clonedRequest, observation.getContext());
    }
}
