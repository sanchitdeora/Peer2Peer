/**
 *
 * To manage each peer concurrently
 *
 */



import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerManager extends Thread {
    Socket socket_request;
    BufferedOutputStream bos;
    BufferedInputStream bis;
    boolean isClient;
    String peerID;
    byte[] peerBitfield;
    boolean isPeerInterested = true;
    boolean isPeerChoked = true;
    AtomicBoolean terminate = new AtomicBoolean(false);
    Float download_rate = 1.0F;
    ConfigParser config;
    MessageManager messagereader;
    MessageManager messagemanager;
    boolean flag = false;

    public PeerManager(Socket s, boolean isCurrentPeerClient, String peerID, ConfigParser config) {

        this.config = config;
        this.socket_request = s;
        this.isClient = isCurrentPeerClient;
        messagereader = new MessageManager();
        messagemanager = new MessageManager();

        try {
            bos = new BufferedOutputStream(socket_request.getOutputStream());
            bos.flush();
            bis = new BufferedInputStream(socket_request.getInputStream());

            if (isCurrentPeerClient) {
                initializeClient(peerID);
            } else {
                initializeServer();
            }

            beginCommunication();

        } catch (IOException e) {
            e.printStackTrace();
            peerProcess.loggers.info("Exception: " + e.toString());
        }
    }

    public void run() {
        try {
            long proc_time = 0l;
            long total_time = 0l;

            byte[] length, type;
            type = new byte[1];
            length = new byte[4];

            int index_req = 0;
            int pieces_received_count = 0;

            while (!terminate.get()) {

                bis.read(length);
                bis.read(type);
                int typeId = new BigInteger(type).intValue();
                MessageTypes types = MessageTypes.values()[typeId];

                //To process the message types of the received messages

                switch (types) {
                    case choke:
                        peerProcess.loggers.info("Peer: " + peerProcess.currPeerID + " is choked by Peer: " + peerID);
                        receiveChoke(index_req);
                        break;

                    case unchoke:
                        peerProcess.loggers.info("Peer: " + peerProcess.currPeerID + " is unchoked by Peer:" + peerID);
                        int piece_Index = messagemanager.pieceIndex(peerProcess.bitfields, peerBitfield, peerProcess.pieces_requested);
                        if (piece_Index >= 0) {
                            index_req = piece_Index;
                            peerProcess.pieces_requested[piece_Index].set(true);
                            sendMessage(messagemanager.request(piece_Index));
                            proc_time = System.nanoTime();
                        }
                        break;

                    case interested:
                        receiveInterested();
                        break;

                    case notInterested:
                        receiveNotInterested();
                        break;

                    case have:
                        receiveHave();
                        break;

                    case request:
                        receiveRequest();
                        break;

                    case piece:
                        byte[] piece_index_byte = new byte[4];
                        bis.read(piece_index_byte);

                        int piece_index = ByteArray.byteArrayToInt(piece_index_byte);

                        int message_length = ByteArray.byteArrayToInt(length);

                        byte[] payload = messagemanager.readPayload(bis, message_length - 5);

                        // Update the bitfield array with received piece

                        peerProcess.bitfields[piece_index / 8] |= 1 << (7 - (piece_index % 8));


                        // Storing the actual piece in the array
                        int begin = piece_index * ConfigParser.piece_size;
                        int i = 0;
                        while(i < payload.length) {
                            peerProcess.pieces_received[begin + i] = payload[i];
                            i++;
                        }
                        pieces_received_count++;
                        peerProcess.loggers.info("Peer: " + peerProcess.currPeerID + " has downloaded the piece " + piece_index + " from Peer: "
                                + peerID + ". Now the number of pieces it has is : " + pieces_received_count);

                        total_time += System.nanoTime() - proc_time;
                        download_rate = (float) ((pieces_received_count * ConfigParser.piece_size) / total_time);

                        broadcastHave(piece_index_byte);


                        // Requesting next piece

                        int next_piece_index = messagemanager.pieceIndex(peerProcess.bitfields, peerBitfield, peerProcess.pieces_requested);

                        if (next_piece_index >= 0) {
                            index_req = next_piece_index;
                            peerProcess.pieces_requested[next_piece_index].set(true);
                            sendMessage(messagemanager.request(next_piece_index));
                            proc_time = System.currentTimeMillis();
                        } else {

                            // if no piece is required, broadcast not interested to all peers

                            verifyBroadcastNotInterested();
                        }

                        break;

                    default:
                        break;
                }

            }
        }

        catch (IOException e) {
            System.exit(0);
        }

    }

    //  To close the socket and set terminate true

    public void closeSocket(boolean stop)  {
        terminate.set(stop);
        if (terminate.get()) {
            peerProcess.loggers.info("Closing Socket");
            closeSocket();
        }
    }

    // To send message to the connected peer

    public void sendMessage(byte[] msg) {
        try {
            bos.write(msg);
            bos.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }

    //Sets the requested piece index to false on receiving Choke message

    public void receiveChoke(int requestedIndex) {
        byte indByte = peerProcess.bitfields[requestedIndex / 8];
        if (((1 << (7 - (requestedIndex % 8))) & indByte) == 0) {
            peerProcess.pieces_requested[requestedIndex].set(false);
        }
    }

    //Sets Interested to true

    public void receiveInterested() {
        peerProcess.loggers.info("Peer " + peerProcess.currPeerID + " received the 'interested' message from " + peerID + ".");
        isPeerInterested = true;

    }

    //Sets Interested to False and Choke to true

    public void receiveNotInterested() {
        if(!socket_request.isClosed() && !flag)
            peerProcess.loggers.info("Peer " + peerProcess.currPeerID + " received the 'not interested' message from Peer: " + peerID);
        isPeerInterested = false;
        isPeerChoked = true;
    }

    //Sets peer bitfield index and checks if current peer interested or not

    public void receiveHave() {

        byte[] piece_index_bytes = messagemanager.readPayload(bis, 4);
        int pieceIndex = ByteArray.byteArrayToInt(piece_index_bytes);
        peerProcess.loggers.info("Peer: " + peerProcess.currPeerID + " received the 'have' message from Peer: " + peerID
                + " for the piece index:" + pieceIndex);

        peerBitfield[pieceIndex / 8] |= (1 << (7 - (pieceIndex % 8)));
        byte indexByte = peerProcess.bitfields[pieceIndex / 8];
        if (((1 << (7 - (pieceIndex % 8))) & indexByte) == 0) {
            sendMessage(messagemanager.interested());
        } else {
            sendMessage(messagemanager.notInterested());
        }
    }

    //To receive the request message and send piece if Unchocked

    public void receiveRequest() {
        byte[] payload = messagemanager.readPayload(bis, 4);

        int piece_index = ByteArray.byteArrayToInt(payload);

        peerProcess.loggers.info("Peer: " + peerProcess.currPeerID + " received the 'request' message from Peer: " + peerID
                + " for the pieceIndex: " + piece_index);
        int start_index = piece_index * ConfigParser.piece_size;

        try {
            byte[] data;

            if ((ConfigParser.file_size - start_index) < ConfigParser.piece_size) {
                data = Arrays.copyOfRange(peerProcess.pieces_received, start_index, ConfigParser.file_size);
            }

            else {
                data = Arrays.copyOfRange(peerProcess.pieces_received, start_index, start_index + ConfigParser.piece_size);
            }

            if (!isPeerChoked) {
                sendMessage(messagemanager.piece(piece_index, data));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }

    }
    //To send and verify handshake when the current peer is sending request

    public void initializeClient(String peerId) {
        this.peerID = peerId;
        Handshake handshake = new Handshake(String.valueOf(peerProcess.currPeerID));
        sendMessage(handshake.constructHandshakeMessage());

        messagemanager.readHandshake(bis);
        peerProcess.loggers.info("Peer "+ peerProcess.currPeerID + " makes a connection to Peer:" + peerID);
    }

    //To send and verify handshake when the current peer is receiving request

    public void initializeServer() {
        this.peerID = messagemanager.readHandshake(bis);
        Handshake handshake = new Handshake(String.valueOf(peerProcess.currPeerID));
        sendMessage(handshake.constructHandshakeMessage());
        peerProcess.loggers.info("Peer: "+ peerProcess.currPeerID + " makes a connection to Peer: " + peerID);
    }

    //To start communicating with a peer, sending bitfield and interested or not

    public void beginCommunication() {
        peerProcess.loggers.info("Peer: "+ peerProcess.currPeerID + " is connected from Peer: " + peerID);
        sendMessage(messagemanager.bitField(peerProcess.bitfields));
        peerBitfield = messagemanager.readBitfield(bis);

        if (messagemanager.ifInterested(peerProcess.bitfields, peerBitfield)) {
            sendMessage(messagemanager.interested());
        } else {
            sendMessage(messagemanager.notInterested());
        }
    }

    //To close the socket

    public void closeSocket()
    {
        flag = true;
        try {
            if (!socket_request.isClosed())
                socket_request.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Create peer folder and update received file

    public void updateFile() throws IOException {
        new File("peer_" + peerProcess.currPeerID).mkdir();
        File file = new File("peer_" + peerProcess.currPeerID + "/" + ConfigParser.file_name);
        FileOutputStream fdata = new FileOutputStream(file);
        fdata.write(peerProcess.pieces_received);
        fdata.close();
        peerProcess.loggers.info("Peer " + peerProcess.currPeerID + " has downloaded the complete file.");
    }

    //Broadcasts have message to peers

    public void broadcastHave(byte[] pieceIndex) {
        for (PeerManager pm : peerProcess.peerManagers) {
            pm.sendMessage(pm.messagemanager.have(pieceIndex));
        }
    }

    //Broadcasts Not Interested message to peers

    public void verifyBroadcastNotInterested() throws IOException {
        sendMessage(messagemanager.notInterested());
        if (Arrays.equals(peerProcess.bitfields, peerProcess.complete_bitfield)) {
            for (PeerManager pm : peerProcess.peerManagers) {
                pm.sendMessage(pm.messagemanager.notInterested());
            }
            updateFile();
        }
    }

}
