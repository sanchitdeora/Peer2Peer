/**
 *
 * To store the values from the configuration files
 *
 */

import java.io.IOException;
import java.util.ArrayList;

public class ConfigParser {
    static Integer num_of_preferred_neighbors;
    static Integer unchoking_interval;
    static Integer optimistic_unchoking_interval;
    static String file_name;
    static Integer file_size;
    static Integer piece_size;
    static Integer port = 8000;
    static ArrayList<Peer> peerInfoList;
    static Integer num_of_pieces;
    static Integer num_of_bytes;

    public ConfigParser() throws IOException
    {
        peerInfoList = new ArrayList<Peer>();
        FileParser.parseCommonConfig();
        FileParser.parsePeerInfo(peerInfoList);
        num_of_pieces = (file_size % piece_size == 0) ? file_size / piece_size : (file_size / piece_size) + 1;
        num_of_bytes = (num_of_pieces % 8 == 0) ? num_of_pieces / 8 : (num_of_pieces / 8) + 1;
    }
}
