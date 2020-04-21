/**
 *
 * To construct, read and process messages
 *
 */


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;


public class MessageManager
{


    public byte[] choke() {
        peerProcess.loggers.info("Constructing CHOKE Message");
        byte[] len = ByteArray.intToByteArray(1);
        byte byt = ByteArray.intToByteArray(MessageTypes.choke.ordinal())[3];
        byte[] res = ByteArray.joinByteArrayWithByte(len, byt);
        return res;
    }

    public byte[] unchoke() {
        peerProcess.loggers.info("Constructing UNCHOKE Message");
        byte[] len = ByteArray.intToByteArray(1);
        byte byt = ByteArray.intToByteArray(MessageTypes.unchoke.ordinal())[3];
        byte[] res = ByteArray.joinByteArrayWithByte(len, byt);
        return res;
    }

    public byte[] interested() {
        peerProcess.loggers.info("Constructing INTERESTED Message");
        byte[] len = ByteArray.intToByteArray(1);
        byte byt = ByteArray.intToByteArray(MessageTypes.interested.ordinal())[3];
        byte[] res = ByteArray.joinByteArrayWithByte(len, byt);
        return res;
    }

    public byte[] notInterested() {
        peerProcess.loggers.info("Constructing NOTINTERESTED Message");
        byte[] len = ByteArray.intToByteArray(1);
        byte byt = ByteArray.intToByteArray(MessageTypes.notInterested.ordinal())[3];
        byte[] res = ByteArray.joinByteArrayWithByte(len, byt);
        return res;
    }

    public byte[] have(byte[] pieceIndex) {
        peerProcess.loggers.info("Constructing HAVE message");
        byte[] len = ByteArray.intToByteArray(5);
        byte byt = ByteArray.intToByteArray(MessageTypes.have.ordinal())[3];
        byte[] res = ByteArray.joinByteArrays(ByteArray.joinByteArrayWithByte(len, byt), pieceIndex);
        return res;
    }

    public byte[] bitField(byte[] payload) {
        peerProcess.loggers.info("Constructing BITFIELD Message");
        Message message = new Message(MessageTypes.bitfield, payload);
        return message.getMessage();
    }

    public byte[] request(int index) {
        peerProcess.loggers.info("Constructing REQUEST Message");
        Message message = new Message(MessageTypes.request, ByteArray.intToByteArray(index));
        return message.getMessage();
    }

    public byte[] piece(int idx, byte[] payload) {
        peerProcess.loggers.info("Constructing PIECE Message");
        Message message = new Message(MessageTypes.piece,
                ByteArray.joinByteArrays(ByteArray.intToByteArray(idx), payload));
        return message.getMessage();
    }




    // Read the received bitfield
    public synchronized byte[] readBitfield(InputStream is) {
        byte[] peerBitField = new byte[0];
        try {
            byte[] length = new byte[4];
            is.read(length);
            peerBitField = readBitfieldPayload(is, ByteArray.byteArrayToInt(length) - 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return peerBitField;
    }

    //Calculating a list of required pieces to request

    public int pieceIndex(byte[] currentBitfield, byte[] peerBitfield,
                          AtomicBoolean[] requestedBitfield) {
        byte[] req = new byte[currentBitfield.length];
        byte[] bytesRequested = ByteArray.booleanArraytoByteArray(requestedBitfield);
        byte[] available = new byte[currentBitfield.length];
        List<Integer> list = new ArrayList<Integer>();
        int i;
        for(i = 0; i < currentBitfield.length; i++ ) {
            available[i] = (byte) (currentBitfield[i] & bytesRequested[i]);
            req[i] = (byte) ((available[i] ^ peerBitfield[i]) & ~available[i]);

            if (req[i] != 0)
                list.add(i);
        }
        return getPieceIndex(list, req);
    }

    //Generate random int within limit

    public int getRnd(int limit) {

        return new Random().nextInt(limit);
    }

    //Randomly choosing a bit

    public int getRandomBit(byte message) {
        int bit_index = getRnd(8);
        int i = 0;
        while (i < 8) {
            if ((message & (1 << i)) != 0) {
                bit_index = i;
                break;
            }
            i++;
        }
        return bit_index;
    }

    //Selecting the piece to request at random

    public int getPieceIndex(List<Integer> l, byte[] req) {
        if(l.isEmpty())
            return -1;
        int byte_index = l.get(getRnd(l.size()));
        byte random = req[byte_index];
        int bit_index = getRandomBit(random);
        return (byte_index * 8) + (7 - bit_index);
    }


    // Read the received payload in bytes from input stream

    public byte[] readPayload(InputStream is, int payloadLength) {
        byte[] result = new byte[0];
        int remainingLength = payloadLength;
        try {
            while (remainingLength != 0) {
                int bytesAvailable = is.available();
                int read_bytes = 0;
                if (payloadLength > bytesAvailable) {
                    read_bytes = bytesAvailable;
                } else {
                    read_bytes = payloadLength;
                }

                byte[] rd = new byte[read_bytes];
                if (read_bytes != 0) {
                    is.read(rd);
                    result = ByteArray.joinByteArrays(result, rd);
                    remainingLength = remainingLength - read_bytes;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    // Read received handshake and returns peer ID

    public String readHandshake(InputStream is) {
        try {
            isHeaderValid(is);
            is.read(new byte[10]);
            byte[] peerId = new byte[4];
            is.read(peerId);
            return new String(peerId);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    // check if the received header is valid

    public void isHeaderValid(InputStream is) throws IOException {
        byte[] header = new byte[18];
        is.read(header);
        if (!(new String(header).equals("P2PFILESHARINGPROJ")))
            throw new RuntimeException("Invalid Header");
    }

    // Check if the current peer is interested in any bitField from the connected peer

    public boolean ifInterested(byte[] currentBitfield, byte[] peerBitfield) {
        byte byteSet;
        int start = 0;
        while (start < currentBitfield.length) {
            byteSet = (byte) (~currentBitfield[start] & peerBitfield[start]);
            if (byteSet != 0) {
                return true;
            }
            start ++;
        }
        return false;
    }

    // Read bitfield payload

    public byte[] readBitfieldPayload(InputStream is,int len) throws IOException {

        byte[] peerBitfield = new byte[len];
        byte[] type = new byte[1];
        is.read(type);
        byte value = ByteArray.intToByteArray(MessageTypes.bitfield.ordinal())[3];
        if(type[0] == value)
        {
            is.read(peerBitfield);
        }
        return peerBitfield;
    }

}