/**
 *
 * To parse the configuration files
 *
 */



import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;


public class FileParser {

    private static BufferedReader br;

    public static void parsePeerInfo(List<Peer> result) throws IOException {
        FileReader fr = new FileReader("PeerInfo.cfg");
        br = new BufferedReader(fr);
        String line = br.readLine();
        while (line != null) {
            String[] values = line.split("\\s+");
            result.add(new Peer(values[0], values[1], values[2], values[3]));
            line = br.readLine();
        }
    }

    public static void parseCommonConfig() throws IOException {

        FileReader fr = new FileReader("Common.cfg");
        br = new BufferedReader(fr);
        String line = br.readLine();
        while (line != null) {
            String[] tokens = line.split("\\s+");
            switch (tokens[0]) {

                case "NumberOfPreferredNeighbors":
                    ConfigParser.num_of_preferred_neighbors = Integer.parseInt(tokens[1]);
                    break;

                case "UnchokingInterval":
                    ConfigParser.unchoking_interval = Integer.parseInt(tokens[1]);
                    break;

                case "OptimisticUnchokingInterval":
                    ConfigParser.optimistic_unchoking_interval = Integer.parseInt(tokens[1]);
                    break;

                case "FileName":
                    ConfigParser.file_name = tokens[1];
                    break;

                case "FileSize":
                    ConfigParser.file_size = Integer.parseInt(tokens[1]);
                    break;

                case "PieceSize":
                    ConfigParser.piece_size = Integer.parseInt(tokens[1]);
                    break;

            }
            line = br.readLine();
        }

    }

}
