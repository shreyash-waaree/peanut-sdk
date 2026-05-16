package com.keenon.sdk.component.gating.data;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/gating/data/GatingType.class */
public enum GatingType {
    FOUR { // from class: com.keenon.sdk.component.gating.data.GatingType.1
        @Override // com.keenon.sdk.component.gating.data.GatingType
        public int[] getGatingIdArr() {
            return new int[]{1, 2, 3, 4};
        }

        @Override // com.keenon.sdk.component.gating.data.GatingType
        public int getTypeId() {
            return 1;
        }
    },
    DOUBLE { // from class: com.keenon.sdk.component.gating.data.GatingType.2
        @Override // com.keenon.sdk.component.gating.data.GatingType
        public int[] getGatingIdArr() {
            return new int[]{1, 3};
        }

        @Override // com.keenon.sdk.component.gating.data.GatingType
        public int getTypeId() {
            return 2;
        }
    },
    THREE { // from class: com.keenon.sdk.component.gating.data.GatingType.3
        @Override // com.keenon.sdk.component.gating.data.GatingType
        public int[] getGatingIdArr() {
            return new int[]{1, 3, 4};
        }

        @Override // com.keenon.sdk.component.gating.data.GatingType
        public int getTypeId() {
            return 3;
        }
    },
    THREE_REVERSE { // from class: com.keenon.sdk.component.gating.data.GatingType.4
        @Override // com.keenon.sdk.component.gating.data.GatingType
        public int[] getGatingIdArr() {
            return new int[]{1, 2, 3};
        }

        @Override // com.keenon.sdk.component.gating.data.GatingType
        public int getTypeId() {
            return 4;
        }
    },
    SINGLE { // from class: com.keenon.sdk.component.gating.data.GatingType.5
        @Override // com.keenon.sdk.component.gating.data.GatingType
        public int[] getGatingIdArr() {
            return new int[]{1};
        }

        @Override // com.keenon.sdk.component.gating.data.GatingType
        public int getTypeId() {
            return 5;
        }
    };

    public abstract int[] getGatingIdArr();

    public abstract int getTypeId();

    public static GatingType getGatingType(int id) {
        if (id == 1) {
            return FOUR;
        }
        if (id == 2) {
            return DOUBLE;
        }
        if (id == 3) {
            return THREE;
        }
        if (id == 4) {
            return THREE_REVERSE;
        }
        if (id == 5) {
            return SINGLE;
        }
        return DOUBLE;
    }
}
