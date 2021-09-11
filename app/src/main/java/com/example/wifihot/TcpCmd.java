package com.example.wifihot;

import static com.example.wifihot.utiles.CRCUtils.calCRC8;

public class TcpCmd {

    public static int CMD_READ_FILE_START = 0xF2;
    public static int CMD_READ_FILE_DATA = 0xF3;


    private static int seqNo = 0;
    private static void addNo() {
        seqNo++;
        if (seqNo >= 255) {
            seqNo = 0;
        }
    }

    public static byte[] readFileStart(byte[] name, int offset) {
        int len = 20;
        byte[] cmd = new byte[8 + len];
        cmd[0] = (byte) 0xA5;
        cmd[1] = (byte) CMD_READ_FILE_START;
        cmd[2] = (byte) ~CMD_READ_FILE_START;
        cmd[3] = (byte) 0x00;
        cmd[4] = (byte) seqNo;
        cmd[5] = (byte) 0x14;
        cmd[6] = (byte) 0x00;
        int k = 0;
        for (k = 0; k < 16; k++) {
            cmd[7 + k] = name[k];
        }
        byte[] temp = intToByteArray(offset);
        for (k = 0; k < 4; k++) {
            cmd[23 + k] = temp[k];
        }
        cmd[27] = calCRC8(cmd);
        addNo();
        return cmd;
    }



    public static byte[] readFileData(int addr_offset) {
        int len = 4;
        byte[] cmd = new byte[8 + len];
        cmd[0] = (byte) 0xA5;
        cmd[1] = (byte) CMD_READ_FILE_DATA;
        cmd[2] = (byte) ~CMD_READ_FILE_DATA;
        cmd[3] = (byte) 0x00;
        cmd[4] = (byte) seqNo;
        cmd[5] = (byte) 0x04;
        cmd[6] = (byte) 0x00;
        int k;
        byte[] temp = intToByteArray(addr_offset);
        for (k = 0; k < 4; k++) {
            cmd[7 + k] = temp[k];
        }

        cmd[11] = calCRC8(cmd);
        addNo();
        return cmd;
    }

    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[3] = (byte) ((i >> 24) & 0xFF);
        result[2] = (byte) ((i >> 16) & 0xFF);
        result[1] = (byte) ((i >> 8) & 0xFF);
        result[0] = (byte) (i & 0xFF);
        return result;
    }

}
