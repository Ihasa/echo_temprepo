//package echowand.sample;
import echowand.info.PropertyConstraintOnOff;
import echowand.object.LocalObjectRandomDelegate;
import echowand.common.EPC;
import echowand.info.TemperatureSensorInfo;
import echowand.logic.TooManyObjectsException;
import echowand.logic.TransactionManager;
import echowand.net.Inet4Subnet;
import echowand.net.Subnet;
import echowand.net.SubnetException;
import echowand.object.LocalObject;
import echowand.object.LocalObjectDateTimeDelegate;
import echowand.object.LocalObjectDefaultDelegate;
import echowand.object.LocalObjectDelegate;
import echowand.object.LocalObjectNotifyDelegate;
import echowand.object.ObjectData;
import echowand.service.Core;
import echowand.service.LocalObjectConfig;
import echowand.service.PropertyUpdater;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An ECHONET Lite Device supporting thermal zone sensors.
 * @author ymakino
 */
public class ServiceThermalZoneDevice {
    public static final String SENSOR_FILENAME = "temperature.txt";
    public static class ThermalZoneDelegate extends LocalObjectNotifyDelegate {

        private File file;

        public ThermalZoneDelegate(Subnet s, TransactionManager m, File file) {
	    super(s, m);
            this.file = file;
        }
	
	private String getLocaleCodes(ObjectData data){
		byte b = data.toBytes()[0];
		StringBuilder sb = new StringBuilder();
		//Appendix.pdf p26
		sb.append((b & 0x80)>>7).append("-"); //1000 0000
		sb.append((b & 0x78)>>3).append("-"); //0111 1000
		sb.append(b & 0x07);		 //0000 0111
		return sb.toString();
	}

	@Override
	public void notifyDataChanged(LocalObjectDelegate.NotifyState result, LocalObject object, EPC epc, ObjectData curData, ObjectData oldData){
		super.notifyDataChanged(result, object,epc,curData,oldData);
		System.out.println("notify:" + epc + "..." + oldData + " -> " + curData);	
		String oldStr="", curStr="";	
		switch(epc){
			case x80:
				oldStr = oldData.toString().equals("30") ? "ON" : "OFF";
				curStr = curData.toString().equals("30") ? "ON" : "OFF";	
				break;
			case x81:
				oldStr = getLocaleCodes(oldData); 
				curStr = getLocaleCodes(curData); 
				break;
			case x88:
				oldStr = oldData.toString().equals("41") ? "FAILURE" : "NORMAL";
				curStr = curData.toString().equals("41") ? "WARNING" : "NORMAL";
				break;
		}
		System.out.println("(" + oldStr + " -> " + curStr + ")");	
	}
	
        @Override
        public void getData(LocalObjectDelegate.GetState result, LocalObject object, EPC epc) {
            if (epc == EPC.xE0) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line = br.readLine();
                    short value = Short.parseShort(line);
                    System.out.println(file  + ": " + line + "(" + (value) + ")");
                   byte b1, b2; 
		    if(value > 32766){
			b1 = (byte) 0x7F;
			b2 = (byte) 0xFF;
		    }else if(value < -2732){
			b1 = (byte) 0x80;
			b2 = (byte) 0x00;
		    }else {
	       	    	b1 = (byte) ((value >> 8) & 0xff);
                    	b2 = (byte) (value & 0xff);
                    } 
		    result.setGetData(new ObjectData(b1, b2));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    object.setInternalData(EPC.x88, new ObjectData((byte)0x41));
                    result.setFail();
                }
            }
        }
    }
    
    public static LocalObjectConfig createThermalZoneConfig(Subnet s, TransactionManager t) throws SubnetException{
        TemperatureSensorInfo info = new TemperatureSensorInfo();

	//add Date information	
	//EPC, gettable, settable, observable, dataSize
        info.add(EPC.x97, true, false, false, 2);
        info.add(EPC.x98, true, false, false, 4);
	//overwrite EPC 0x80
	//EPC,gettable,settable,observable,initial data, constraint	
	info.add(EPC.x80, true, true, true, new byte[]{0x30}, new PropertyConstraintOnOff());
	LocalObjectConfig config = new LocalObjectConfig(info);
        config.addDelegate(new LocalObjectDateTimeDelegate());
	
       	config.addDelegate(new ThermalZoneDelegate(s,t,new File(SENSOR_FILENAME)));
	//switch on off by 3s interval
        config.addPropertyUpdater(new PropertyUpdater(3000){
		@Override
		public void loop(LocalObject lo){
			int b = lo.getData(EPC.x80).equals(new ObjectData((byte)0x30)) ? 0x31 : 0x30;
			lo.setData(EPC.x80, new ObjectData((byte)b));
		}
	});
	return config;
    }
    
    public static void main(String[] args) throws InterruptedException, SocketException, SubnetException, TooManyObjectsException {
        if (args.length > 1) {
            System.err.println("Usage: ServiceThermalZoneDevice [ifname]");
            return;
        }
        
        Core core;
       Subnet s; 
        if (args.length == 0) {
            s = Inet4Subnet.startSubnet();
        } else {
            NetworkInterface nif = NetworkInterface.getByName(args[0]);
	    s = Inet4Subnet.startSubnet(nif);
        }
        core = new Core(s);
	core.addLocalObjectConfig(createThermalZoneConfig(s,new TransactionManager(s)));
        core.startService();
    }
}
