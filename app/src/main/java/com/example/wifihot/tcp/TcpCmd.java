package com.example.wifihot.tcp;

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

    public static byte[] readFileStart() {
        int len = 0;
        byte[] cmd = new byte[8 + len];
        cmd[0] = (byte) 0xA5;
        cmd[1] = (byte) CMD_READ_FILE_START;
        cmd[2] = (byte) ~CMD_READ_FILE_START;
        cmd[3] = (byte) 0x00;
        cmd[4] = (byte) seqNo;
        cmd[5] = (byte) 0x00;
        cmd[6] = (byte) 0x00;
        cmd[7] = calCRC8(cmd);
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
        byte[] temp = intToByteArray(addr_offset);
        for (int k = 0; k < 4; k++) {
            cmd[7 + k] = temp[k];
        }
        cmd[11] = calCRC8(cmd);
        addNo();
        return cmd;
    }



    public static byte[] ReplyFileStart(int size,int seq) {
        int len = 4;
        byte[] cmd = new byte[8 + len];
        cmd[0] = (byte) 0xA5;
        cmd[1] = (byte) CMD_READ_FILE_START;
        cmd[2] = (byte) ~CMD_READ_FILE_START;
        cmd[3] = (byte) 0x00;
        cmd[4] = (byte) seq;
        cmd[5] = (byte) 0x04;
        cmd[6] = (byte) 0x00;
        byte[] temp = intToByteArray(size);
        for (int k = 0; k < 4; k++) {
            cmd[7 + k] = temp[k];
        }
        cmd[11] = calCRC8(cmd);
        addNo();
        return cmd;
    }


    public static byte[] ReplyFileData(byte[] contents,int seq) {
        int len = contents.length;
        byte[] cmd = new byte[8 + len];
        cmd[0] = (byte) 0xA5;
        cmd[1] = (byte) CMD_READ_FILE_DATA;
        cmd[2] = (byte) ~CMD_READ_FILE_DATA;
        cmd[3] = (byte) 0x00;
        cmd[4] = (byte) seq;
        byte[] temp = shortToByteArray(len);
        cmd[5] = (byte) temp[0];
        cmd[6] = (byte) temp[1];
        for (int k = 0; k < len; k++) {
            cmd[7 + k] = contents[k];
        }
        cmd[7+len] = calCRC8(cmd);
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

    public static byte[] shortToByteArray(int i) {
        byte[] result = new byte[2];
        result[1] = (byte) ((i >> 8) & 0xFF);
        result[0] = (byte) (i & 0xFF);
        return result;
    }
}
