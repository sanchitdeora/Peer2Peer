/**
 *
 * To get parts of messages
 *
 */

public class Message {
    int length;
    MessageTypes type;
    byte[] payload;

    public Message(MessageTypes type, byte[] payload) {

        this.length = payload.length;
        this.type = type;
        this.payload = payload;

    }

    public int getLength() {
        return length;
    }
    public void setLength(int length) {
        this.length = length;
    }
    public MessageTypes getType() {
        return type;
    }
    public void setType(MessageTypes type) {
        this.type = type;
    }
    public byte[] getPayload() {
        return payload;
    }
    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public byte[] getMessage() {
        Integer msgLength = getLength() + 1;
        byte[] len = ByteArray.intToByteArray(msgLength);
        byte byt = ByteArray.intToByteArray(getType().ordinal())[3];
        byte[] res = ByteArray.joinByteArrays(ByteArray.joinByteArrayWithByte(len, byt), getPayload());
        return res;
    }
}
