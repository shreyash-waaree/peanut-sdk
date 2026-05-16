package com.keenon.peanut.sample.receiver;

import com.keenon.sdk.constant.ApiConstants;

/**
 * Parses incoming WebSocket command strings into motor direction constants.
 * Uses the exact same ApiConstants.MotorMove values that MotorDemo uses.
 *
 * Command mapping:
 *   "FRONT" → ApiConstants.MotorMove.FRONT (1)
 *   "BACK"  → ApiConstants.MotorMove.BACK  (2)
 *   "LEFT"  → ApiConstants.MotorMove.LEFT  (3)
 *   "RIGHT" → ApiConstants.MotorMove.RIGHT (4)
 *   "STOP"  → COMMAND_STOP (-1)
 *   unknown → COMMAND_UNKNOWN (-2)
 */
public class CommandParser {

    public static final int COMMAND_STOP = -1;
    public static final int COMMAND_UNKNOWN = -2;

    /**
     * Parse a raw WebSocket message into a motor direction integer.
     *
     * @param rawMessage the raw string from the WebSocket client
     * @return one of ApiConstants.MotorMove.{FRONT,BACK,LEFT,RIGHT}, COMMAND_STOP, or COMMAND_UNKNOWN
     */
    public static int parse(String rawMessage) {
        if (rawMessage == null) {
            return COMMAND_UNKNOWN;
        }
        String cmd = rawMessage.trim().toUpperCase();
        switch (cmd) {
            case "FRONT":
            case "FORWARD":
                return ApiConstants.MotorMove.FRONT;
            case "BACK":
            case "BACKWARD":
                return ApiConstants.MotorMove.BACK;
            case "LEFT":
                return ApiConstants.MotorMove.LEFT;
            case "RIGHT":
                return ApiConstants.MotorMove.RIGHT;
            case "STOP":
                return COMMAND_STOP;
            default:
                return COMMAND_UNKNOWN;
        }
    }

    /**
     * Convert a direction integer back to a human-readable label.
     */
    public static String directionToString(int direction) {
        switch (direction) {
            case ApiConstants.MotorMove.FRONT:
                return "FRONT";
            case ApiConstants.MotorMove.BACK:
                return "BACK";
            case ApiConstants.MotorMove.LEFT:
                return "LEFT";
            case ApiConstants.MotorMove.RIGHT:
                return "RIGHT";
            case COMMAND_STOP:
                return "STOP";
            default:
                return "UNKNOWN";
        }
    }
}
