
public class Peer {
    public String id;
    public String address;
    public String port;
    public String hasFile;

    public Peer(String id, String ip, String port, String hasFile) {
        this.id = id;
        address = ip;
        this.port = port;
        this.hasFile = hasFile;
    }
}
