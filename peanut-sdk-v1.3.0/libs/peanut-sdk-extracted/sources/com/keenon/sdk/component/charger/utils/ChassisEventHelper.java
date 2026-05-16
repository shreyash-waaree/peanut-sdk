package com.keenon.sdk.component.charger.utils;

import com.keenon.sdk.component.ScheduleComponent;
import com.keenon.sdk.component.charger.common.ChargerEventCode;
import com.keenon.sdk.constant.ApiConstants;
import com.keenon.sdk.scmIot.protopack.base.ProtoDev;
import com.keenon.sdk.scmIot.protopack.base.ProtoTopic;
import org.apache.commons.net.tftp.TFTP;
import org.eclipse.californium.core.coap.NoResponseOption;
import org.eclipse.californium.core.coap.OptionNumberRegistry;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/charger/utils/ChassisEventHelper.class */
public class ChassisEventHelper {
    public static int getChargeEvent(int event) {
        switch (event) {
            case 1:
                return ChargerEventCode.ARRIVED_CHARGE_POINT;
            case 2:
                return ChargerEventCode.NO_MATCH_TO_CHARGING_PILE_TIMEOUT;
            case 3:
                return ChargerEventCode.MATCH_TO_CHARGING_PILE_AT_LEAST_ONE_TIME;
            case 4:
                return ChargerEventCode.NO_LABEL;
            case 5:
                return ChargerEventCode.NO_RADAR;
            case 6:
                return ChargerEventCode.NO_STM32;
            case 7:
                return ChargerEventCode.UNDEFINED_ERR;
            case 8:
                return ChargerEventCode.NO_5V;
            case 9:
                return ChargerEventCode.LOW_CURRENT;
            case 10:
                return ChargerEventCode.CHARGE_INTERRUPT;
            case 11:
                return ChargerEventCode.CHARGE_SUCCESS;
            case 12:
                return ChargerEventCode.GIVE_UP_CHARGE;
            case 13:
                return ChargerEventCode.BACKING_TO_CHARGE_POINT;
            case 14:
                return ChargerEventCode.BACKED_TO_CHARGE_POINT;
            case 15:
                return ChargerEventCode.FORCED_CHARGING;
            case 16:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case OptionNumberRegistry.SIZE2 /* 28 */:
            case ProtoTopic.CONTROL_CALL_BELL /* 29 */:
            case ProtoTopic.CONTROL_ACK /* 30 */:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case ProtoDev.SENSOR_SINGLE_LIGHT_4 /* 36 */:
            case ProtoDev.SENSOR_COOLING_FAN_1 /* 37 */:
            case ProtoDev.SENSOR_COOLING_FAN_2 /* 38 */:
            case 39:
            case 40:
            case 41:
            case 42:
            case ProtoDev.SENSOR_PLASMA_FAN /* 43 */:
            case ProtoDev.SENSOR_UV_LAMP_1 /* 44 */:
            case ProtoDev.SENSOR_UV_LAMP_2 /* 45 */:
            case 46:
            case 47:
            case ProtoDev.SENSOR_UV_LAMP_5 /* 48 */:
            case 49:
            case 50:
            case 52:
            case ProtoDev.SENSOR_BATTERY /* 56 */:
            case ProtoDev.SENSOR_JACKING /* 57 */:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
            case 64:
            case ProtoDev.SENSOR_HEAD_MOTOR /* 66 */:
            case 67:
            case 68:
            case TFTP.DEFAULT_PORT /* 69 */:
            case ScheduleComponent.FORBID /* 70 */:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
            case 80:
            default:
                return 40000;
            case 17:
                return ChargerEventCode.AUTO_CHARGE_MODEL;
            case NoResponseOption.SUPPRESS_ALL /* 26 */:
                return ChargerEventCode.STM32_CANCEL_CHARGE;
            case OptionNumberRegistry.BLOCK1 /* 27 */:
                return ChargerEventCode.ADAPTER_CHARGE;
            case 51:
                return ChargerEventCode.TIME_OUT;
            case ProtoDev.SENSOR_COLOR_LIGHT_4 /* 53 */:
                return ChargerEventCode.CHARGING;
            case ProtoDev.SENSOR_COLOR_LIGHT_5 /* 54 */:
                return ChargerEventCode.CHARGE_DISCONNECT;
            case ProtoDev.SENSOR_COLOR_LIGHT_6 /* 55 */:
                return ChargerEventCode.CHARGE_COMPLETE;
            case ProtoDev.SENSOR_CALL_BELL /* 65 */:
                return ChargerEventCode.EXIT_CHARGE_MODEL;
            case ApiConstants.CODE_NO_PATH /* 81 */:
                return ChargerEventCode.MANUAL_CHARGE_MODEL;
        }
    }
}
