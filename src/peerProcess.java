/**
 *
 * peerProcess is used to initiate the project
 *
 */




import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class peerProcess {

    static List<PeerManager> peerManagers = new CopyOnWriteArrayList<>();
    static PeerManager optimstcUnchokdNeigbor;
    static byte[] bitfields, pieces_received, complete_bitfield;
    static AtomicBoolean[] pieces_requested;
    static Logger loggers;

    ScheduledExecutorService task_scheduler = Executors.newScheduledThreadPool(3);
    Integer port = 8000;
    static Integer currPeerID;
    static ServerSocket serverSocket;

    public static void main(String[] args) throws Exception {

        peerProcess peerprocess = new peerProcess();
        currPeerID = Integer.parseInt(args[0]);

        ConfigParser config = new ConfigParser();
        loggers = LogWriter.getLogger(currPeerID);

        List<Peer> connection_estabhed_peers = new ArrayList<Peer>();
        List<Peer> peers_to_connect = new ArrayList<Peer>();

        boolean is_file_available = false;

        //To parse the configuration files

        for (Peer peer : ConfigParser.peerInfoList) {
            if (Integer.parseInt(peer.id) < currPeerID) {
                connection_estabhed_peers.add(peer);
            } else if (Integer.parseInt(peer.id) == currPeerID) {
                peerprocess.port = Integer.parseInt(peer.port);
                if (peer.hasFile.equals("1"))
                    is_file_available = true;
            } else {
                peers_to_connect.add(peer);
            }
        }


        bitfields = new byte[ConfigParser.num_of_bytes];
        pieces_requested = new AtomicBoolean[ConfigParser.num_of_pieces];
        Arrays.fill(pieces_requested, new AtomicBoolean(false));
        pieces_received = new byte[ConfigParser.file_size];
        complete_bitfield = new byte[ConfigParser.num_of_bytes];
        variableInitialization(is_file_available, ConfigParser.num_of_pieces);
        listeningToPeersConnected(connection_estabhed_peers, config);
        serverSocket = new ServerSocket(peerprocess.port);
        loggers.info("Server Socket listening on port: " + peerprocess.port);
        listeningToPeersInFuture(peers_to_connect, config);
        optimiticallyUnchokeNeighor();
        startTaskSchedulers(peerprocess);
    }


    //To initialize the current and complete bitfield for this peer

    public static void variableInitialization(boolean file_available, int pieces) throws IOException {
        Arrays.fill(complete_bitfield, (byte) 255);
        if (file_available) {
            readFilePayload();
            Arrays.fill(bitfields, (byte) 255);
            if (pieces % 8 != 0) {
                int ending = (int) pieces % 8;
                bitfields[bitfields.length - 1] = 0;
                complete_bitfield[bitfields.length - 1] = 0;
                while (ending != 0) {
                    bitfields[bitfields.length - 1] |= (1 << (8 - ending));
                    complete_bitfield[bitfields.length - 1] |= (1 << (8 - ending));
                    ending --;
                }
            }
        } else {
            if (pieces % 8 != 0) {
                int ending = (int) pieces % 8;
                complete_bitfield[bitfields.length - 1] = 0;
                while (ending != 0) {
                    complete_bitfield[bitfields.length - 1] |= (1 << (8 - ending));
                    ending--;
                }
            }
        }
    }

    //To read the available file payload

    public static void readFilePayload() throws IOException {
        try {
            File resources = new File("peer_" + peerProcess.currPeerID + "/" + ConfigParser.file_name);
            FileInputStream file_payload = new FileInputStream(resources);
            file_payload.read(pieces_received);
            file_payload.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    //To establish connection with the peers before the current Peer in the file

    public static void listeningToPeersConnected(List<Peer> connect_established_peers, ConfigParser config) {

        for (Peer p : connect_established_peers) {
            try {
                PeerManager pm = new PeerManager(new Socket(p.address, Integer.parseInt(p.port)),
                        true, p.id, config);

                pm.start();
                peerManagers.add(pm);
                loggers.info("Peer " + currPeerID + " makes a connection to Peer " + p.id + ".");
            } catch (Exception ex) {
                ex.printStackTrace();
                loggers.info(ex.toString());
            }

        }

    }

    //To establish connection with the peers after the current Peer in the file

    public static void listeningToPeersInFuture(List<Peer> peers_to_connect, ConfigParser config) {
        try {
            for (Peer p : peers_to_connect) {
                Runnable peer_connection = () -> {
                    try {
                        PeerManager future_peers = new PeerManager(serverSocket.accept(), false, p.id,
                                config);
                        loggers.info(
                                "Peer " + currPeerID + " is connected from Peer " + p.id + ".");
                        peerManagers.add(future_peers);
                        future_peers.start();
                    } catch (IOException e) {
                        loggers.info(e.getMessage());
                    }
                };
                new Thread(peer_connection).start();
            }
        } catch (Exception ex) {
            loggers.info("Exception while listening to future peers :" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    //Initially, to optimistically unchoke neighbors

    public static void optimiticallyUnchokeNeighor() {
        List<PeerManager> interstd_neighbour_choked = new ArrayList<PeerManager>();

        for (PeerManager pm : peerManagers) {
            if (pm.isPeerInterested && pm.isPeerChoked) {
                interstd_neighbour_choked.add(pm);
            }
        }
        //If List not empty, optimitically unchoked neighbour is chosen at random
        if (interstd_neighbour_choked.isEmpty()) {
            optimstcUnchokdNeigbor = null;
        } else {
            optimstcUnchokdNeigbor = interstd_neighbour_choked
                    .get(new Random().nextInt(interstd_neighbour_choked.size()));
        }
    }



    //To select the preferred neighbours based on download rate

    public void selectPrefNeighbors(int preferred_neigbor_count) {
        try {
            Collections.sort(peerManagers, (pm1, pm2) -> pm2.download_rate.compareTo(pm1.download_rate));
            int counter = 0;
            List<String> prefList = new ArrayList<String>();

            for (PeerManager pm : peerManagers) {
                if (pm.isPeerInterested) {
                    if (counter < preferred_neigbor_count) {
                        if (pm.isPeerChoked) {
                            pm.isPeerChoked = false;
                            pm.sendMessage(pm.messagemanager.unchoke());
                        }
                        prefList.add(pm.peerID);
                    } else {

                        if (!pm.isPeerChoked && pm != optimstcUnchokdNeigbor) {
                            pm.isPeerChoked = true;
                            pm.sendMessage(pm.messagemanager.choke());
                        }
                    }

                    counter++;
                }
            }
            loggers.info("Peer " + currPeerID + " has the preferred neighbors:" + prefList);
        } catch (Exception e) {
            loggers.info(e.toString());
        }
    }

    // To schedule the selectPrefNeighbors method thread

    public void beginPrefNeibourSchedulr(int k, int p) {
        Runnable searchPrefNeigbours = () -> {
            selectPrefNeighbors(k);
        };
        task_scheduler.scheduleAtFixedRate(searchPrefNeigbours, p, p, TimeUnit.SECONDS);
    }


    // To schedule the selectOptimisticallyPrefNeighbors method thread

    public void beginOptimisticallyPrefNeibourSchedulr(int m) {

        Runnable searchOptimisticallyPrefNeighbours = () -> {
            selectOptimisticallyPrefNeighbors();
        };
        task_scheduler.scheduleAtFixedRate(searchOptimisticallyPrefNeighbours, m, m, TimeUnit.SECONDS);
    }

    //To select the optimistically preferred neighbors

    public void selectOptimisticallyPrefNeighbors() {
        try {

            List<PeerManager> interstd_chok_neighbour = new ArrayList<PeerManager>();

            for (PeerManager pm : peerManagers) {
                if (pm.isPeerInterested && pm.isPeerChoked) {
                    interstd_chok_neighbour.add(pm);
                }
            }

            if (!interstd_chok_neighbour.isEmpty()) {
                if (optimstcUnchokdNeigbor != null && !interstd_chok_neighbour.contains(optimstcUnchokdNeigbor)) {
                    optimstcUnchokdNeigbor.isPeerChoked = true;
                    optimstcUnchokdNeigbor.sendMessage(optimstcUnchokdNeigbor.messagemanager.choke());
                }

                //If List not empty, optimitically unchoked neighbour is chosen at random
                optimstcUnchokdNeigbor = interstd_chok_neighbour
                        .get(new Random().nextInt(interstd_chok_neighbour.size()));
                optimstcUnchokdNeigbor.isPeerChoked = false;
                optimstcUnchokdNeigbor.sendMessage(optimstcUnchokdNeigbor.messagemanager.unchoke());

            } else {
                if (optimstcUnchokdNeigbor != null) {
                    if (!optimstcUnchokdNeigbor.isPeerChoked) {
                        optimstcUnchokdNeigbor.isPeerChoked = true;
                        optimstcUnchokdNeigbor.sendMessage(optimstcUnchokdNeigbor.messagemanager.choke());
                    }
                    optimstcUnchokdNeigbor = null;
                }
            }

            if (optimstcUnchokdNeigbor != null)
                loggers.info("Peer: " + currPeerID + " has the optimistically unchoked neighbor Peer: "
                        + optimstcUnchokdNeigbor.peerID);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    //To concurrently start checking Preferred neighbours, Optimistically Unchoked Neighbours, If File transfer is complete or Not
    public static void startTaskSchedulers(peerProcess peer_process) {
        peer_process.beginPrefNeibourSchedulr(ConfigParser.num_of_preferred_neighbors, ConfigParser.unchoking_interval);
        peer_process.beginOptimisticallyPrefNeibourSchedulr(ConfigParser.optimistic_unchoking_interval);
        peer_process.ifFileTransferDone();
    }


    //To check if the file transfer between all peers is complete

    public void ifFileTransferDone() {
        Runnable checkPeerFileStatus = () -> {
            verifyTerminateSocket(ifEveryPeerReceivedFile());
        };
        task_scheduler.scheduleAtFixedRate(checkPeerFileStatus, 10, 5, TimeUnit.SECONDS);
    }

    //To check if each peer received the entire file

    public boolean ifEveryPeerReceivedFile() {
        boolean isRcvd = true;
        for (PeerManager pm : peerManagers) {
            if (!Arrays.equals(pm.peerBitfield, complete_bitfield)) {
                loggers.info("Peer " + pm.peerID + " yet to receive the entire file.");
                isRcvd = false;
                break;
            }
        }
        return isRcvd;
    }

    //To terminate the socket and schedulers if the file transfer complete

    public void verifyTerminateSocket(boolean isSent) {
        loggers.info("Complete File Status: " + isSent);
        if (isSent && Arrays.equals(bitfields, complete_bitfield)) {
            for (PeerManager pm : peerManagers) {
                pm.closeSocket(true);
            }
            task_scheduler.shutdown();
            try {
                if (!serverSocket.isClosed())
                    serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                loggers.info("Exception when socket closed");
            } finally {
                loggers.info("Terminating PeerProcess with peerId: " + currPeerID);
                System.exit(0);
            }
        }
    }



}