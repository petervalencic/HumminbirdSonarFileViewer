
import java.io.DataInputStream;
import java.io.File;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import com.data.Ping;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Peter
 */
public class Main {

    private static final double RAD_CONVERSION = 180 / Math.PI;
    private static final double EARTH_RADIUS = 6356752.3142;
    private static File datafile = null;
    private static File seconddatafile = null;
    private static File xfile = null;
    private static int blocksize = 0;
    private static List<Integer> index = null;
    private static List<Integer> secondindex = null;
    private static List<Integer> thirdindex = null;

    private static int timestamp;
    private static int longitude;
    private static int latitude;

    //output picture height (modify this)
    static int height = 5352;

    //modify this to obtain different color (RGB value)
    //static Color barva = new Color(179, 255, 153);

    static Color barva = new Color(253, 13, 13);


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            File file = new File("C:/Documents and Settings/Peter/Desktop/peter/RECORD/Piramida-rex.dat");
            String name = initFromDAT(file);
            String dirname = name.substring(0, name.length() - 4);
            String path = file.getParent() != null ? file.getParent() + "/" + dirname : dirname;

            System.out.println("name: " + name);
            System.out.println("dirname: " + dirname);
            System.out.println("path: " + path);

            index = getIDXData(new File(String.format("%s/B002.idx", path)));
            datafile = new File(String.format("%s/B002.SON", path));

            secondindex = getIDXData(new File(String.format("%s/B003.idx", path)));
            seconddatafile = new File(String.format("%s/B003.SON", path));

            thirdindex = getIDXData(new File(String.format("%s/B001.idx", path)));
            xfile = new File(String.format("%s/B001.SON", path));

            System.out.println("Index length: " + getLength() + " pings");

            Ping[] pingRange = getPingRange(0, (int) getLength() - 1);
            System.out.println("pingRange length: " + pingRange.length);

            //shranimo sliko
            
           
            BufferedImage image = new BufferedImage((int)getLength() - 1, height, BufferedImage.TYPE_INT_RGB);
            for (int loop = 0; loop < (int)getLength() - 1; loop++) {
                byte[] soundings = pingRange[loop].getSoundings();
                System.out.println("soundings len:" + soundings.length);
                //gremo čez podatke iz zapisa
                for (int i = 0; i < height; i++) {
                    int mapped = (int) ((i * soundings.length) / (double) height);
                    
                    byte sounding = soundings[mapped];
                    
                    int color = (barva.getRGB() & sounding) | (barva.getRGB() & (sounding << 8))
                            | (barva.getRGB() & (sounding << 16));

                     int pix = 0xFF000000 | sounding << 16 & 0xFF0000 | sounding << 8 & 0xFF00 | sounding & 0xFF;



                    //set pixel color
                    image.setRGB(loop, i, color);
                    //image.setRGB(loop, i, pix);
                    
                }

            }
            File outputfile = new File("c:/image.jpg");
            ImageIO.write(image, "jpg", outputfile);



        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    

    static long getLength() {
        return index.size();
    }

    public int getTimeStamp() {
        return this.timestamp;
    }

    static double getLongitude() {
        return toLongitude(longitude);
    }

    static double getLatitude() {
        return toLatitude(latitude);
    }

    static List<Integer> getIDXData(File idxfile) throws FileNotFoundException,
            IOException {
        DataInputStream stream = new DataInputStream(new FileInputStream(idxfile));
        try {
            List<Integer> index = new ArrayList<Integer>();
            System.out.println("### BEGIN  INDEX");
            
            while (stream.available() > 0) {
             
                int time = stream.readInt(); //no need //preberemo 4 byte
                int offset = stream.readInt(); //preberemo 4 byte
                index.add(offset);
              
            }
            System.out.println("### END  INDEX");
            System.out.println("Index length: " + index.size() );
            return index;
        } finally {
            stream.close();
        }
    }

    /**
     * 
     * @param file
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    static String initFromDAT(File file) throws FileNotFoundException, IOException {

        System.out.println("File: " + file.toString());
        DataInputStream stream = new DataInputStream(new FileInputStream(file));
        try {
            stream.skipBytes(20);
            timestamp = stream.readInt(); //preberemo 4 byte
            longitude = stream.readInt(); //preberemo 4 byte
            latitude = stream.readInt(); //preberemo 4 byte

            System.out.println("timestamp: " + timestamp);
            System.out.println("longitude: " + longitude + " " + toLongitude(longitude));
            System.out.println("latitude: " + latitude + " " + toLatitude(latitude));


            byte[] namebytes = new byte[10];
            stream.read(namebytes, 0, 10); //preberemo 10 bytov
            System.out.println("--->"+new String(namebytes));
            String filename = new String(namebytes);
            stream.skipBytes(2); //skip null character \0000

            int ks = stream.readInt(); //don't know what this is
            int tk = stream.readInt(); //don't know what this is
            blocksize = stream.readInt();
            return filename;
        } finally {
            stream.close();
        }
    }

    /**
     * Convert Lowrance/Humminbird mercator meter format into WGS84.
     * Used this article as a reference: http://www.oziexplorer3.com/eng/eagle.html
     * @return
     */
    static double toLongitude(int mercator) {
        return mercator / EARTH_RADIUS * RAD_CONVERSION;
    }

    static double toLatitude(int mercator) {
        double temp = mercator / EARTH_RADIUS;
        temp = Math.exp(temp);
        temp = (2 * Math.atan(temp)) - (Math.PI / 2);
        return temp * RAD_CONVERSION;
    }

    static void reverseBytes(byte[] array) {
        int lastindex = array.length - 1;
        for (int loop = 0; loop < array.length / 2; loop++) {
            byte temp = array[loop];
            array[loop] = array[lastindex - loop];
            array[lastindex - loop] = temp;
        }
    }

    static Ping[] getPingRange(int offset, int length) throws IOException {

        //With side images soundings needs to be combined into one array.
        //Assumption is that both channels have same amount of samples.
        HumminbirdPing[] firstChannel = getPingRangeFromFile(offset, length, datafile, index);
        HumminbirdPing[] secondChannel = getPingRangeFromFile(offset, length, seconddatafile, secondindex);
        //HumminbirdPing[] thirdChannel = getPingRangeFromFile(offset, length, xfile, thirdindex);


        for (int loop = 0; loop < firstChannel.length; loop++) {
            HumminbirdPing first = firstChannel[loop];
            HumminbirdPing second = secondChannel[loop];
           // HumminbirdPing third = thirdChannel[loop];
            

            //združi oba kanala v en array in vrne rezultat
            byte[] firstsoundings = first.getSoundings();
            byte[] secondsoundings = second.getSoundings();
           // byte[] thirdsoundings = third.getSoundings();

            //reverseBytes(firstsoundings);

            //tukaj je zrdužen levi kanal + desni
            byte[] soundings = new byte[firstsoundings.length + secondsoundings.length ];
            System.arraycopy(firstsoundings, 0, soundings, 0, firstsoundings.length);
            System.arraycopy(secondsoundings, 0, soundings, firstsoundings.length, secondsoundings.length);
           
            first.setSoundings(soundings);
        }


        return firstChannel;

    }

    static HumminbirdPing[] getPingRangeFromFile(int offset, int length, File file, List<Integer> index) throws IOException {
        System.out.println("Humminbird ping name:  " + file.toString());
        System.out.println("offset: " + offset);
        System.out.println("length: " + length);
        System.out.println("block size: " + blocksize);
        RandomAccessFile raf = new RandomAccessFile(file, "r");

        try {
            HumminbirdPing[] pings = new HumminbirdPing[length];
            for (int loop = 0; loop < length; loop++) {
                //v zapisu se premikamo za offset + loop
                raf.seek(index.get(offset + loop));
                // preberemo zapis za določen index
                HumminbirdPing ping = new HumminbirdPing(raf, blocksize);
                pings[loop] = ping;
            }
            return pings;
        } finally {
            raf.close();
        }
    }

    static class HumminbirdPing implements Ping {

        private int time;
        private int longitude;
        private int latitude;
        private short speed;
        private short heading;
        private byte[] soundings;
        private int globina;
        private int beam;

        public HumminbirdPing(RandomAccessFile stream, int blocksize) throws IOException {
          
            stream.skipBytes(10); //premaknemo se za 10 bytov
            time = stream.readInt(); // preberemo 4 byte
            stream.skipBytes(1); //premaknemo se za 1 byte
            longitude = stream.readInt(); //preberemo 4 byte
            stream.skipBytes(1); //premaknemo za 1 byte
            latitude = stream.readInt();
            stream.skipBytes(3);
            heading = stream.readShort(); // preberemo 2 byta
            stream.skipBytes(3);
            speed = stream.readShort(); // preberemo 2 byta
            stream.skipBytes(1);
            globina = stream.readInt(); System.out.println("globina: " + globina);
            stream.skipBytes(1);
            beam = stream.readByte();System.out.println("beam: " + beam);
            stream.skipBytes(3);
                    //stream.skipBytes(5);
            int freq = stream.readInt();// preberemo 4 byte
            stream.skipBytes(10);
            int son = stream.readInt();// preberemo 4 byte
            stream.skipBytes(1);

            //vrnemo zapis, ki ga izrisujemo
            System.out.println("soundings: " + (blocksize-58));
            soundings = new byte[blocksize - 58];
            stream.read(soundings, 0, blocksize - 58);
        }

        public byte[] getSoundings() {
            return soundings;
        }

        public void setSoundings(byte[] soundings) {
            this.soundings = soundings;
        }

        public float getLowLimit() {
            // Humminbird file does not provide this
            return 0;
        }

        public float getTemp() {
            // Humminbird file does not provide this
            return 0;
        }

        public float getDepth() {
            // Humminbird file does not provide this
            return 0;
        }

        public int getTimeStamp() {
            return this.time;
        }

        public float getSpeed() {
            return this.speed * 3.6f;
        }

        public float getTrack() {
            return this.heading / 10.0f;
        }

        public double getLongitude() {
            return toLongitude(longitude);
        }

        public double getLatitude() {
            return toLatitude(latitude);
        }
    }
}
