import echowand.object.*;
import echowand.net.*;
import echowand.logic.*;
import echowand.service.*;
import echowand.common.*;
import echowand.info.NodeProfileInfo;

public class SendData{
	//objのEOJとすべてのプロパティを出力
	static void printEpcs(RemoteObject obj)throws EchonetObjectException{
		//そのオブジェクトのすべてのEPC
        for (int i = 0x80; i <= 0xff; i++) {
        	EPC epc = EPC.fromByte((byte) i);
            if (obj.isGettable(epc)) {
				System.out.println("--" + obj.getEOJ());
            	System.out.println("----" + epc + " " + obj.getData(epc));
            }
        }
	}

	//サブネット内にあるすべてのオブジェクトのすべてのプロパティを出力
	static void printAllEojState(Subnet subnet) throws EchonetObjectException, InterruptedException, SubnetException, TooManyObjectsException{
		RemoteObjectManager remoteManager = new RemoteObjectManager();
		LocalObjectManager localManager = new LocalObjectManager();
		TransactionManager transactionManager = new TransactionManager(subnet);
		InstanceListRequestExecutor executor = new InstanceListRequestExecutor(
			subnet,
			transactionManager,
			remoteManager
		);
        
        //プロファイルオブジェクトを作成
        LocalObject nodeProfileObject = new LocalObject(new NodeProfileInfo());
        nodeProfileObject.addDelegate(new NodeProfileObjectDelegate(localManager));
        nodeProfileObject.addDelegate(new LocalObjectNotifyDelegate(subnet, transactionManager));
        //ローカル（デバイス上）で動くオブジェクトとして追加
        localManager.add(nodeProfileObject);
        
        //echonetオブジェクトとしての動作を開始
        MainLoop mainLoop = new MainLoop();
        mainLoop.setSubnet(subnet);
        mainLoop.addListener(transactionManager);
        Thread mainThread = new Thread(mainLoop);
        mainThread.start();
        
		//情報取得実行
		executor.execute();
		System.out.println("executed");
		//上のが終わるまで待つ
		executor.join();
		System.out.println("joined");
		System.out.println("" +  remoteManager.getNodes().size());
		//サブネット内の全部のノード取得
		for (Node node : remoteManager.getNodes()) {
			System.out.println("node : " + node);
			//そのノード内の全部のオブジェクト
			for(RemoteObject obj : remoteManager.getAtNode(node)){
				printEpcs(obj);
			}
        }
	}
	
	//引数で指定したEOJを持つオブジェクトのすべてのプロパティを出力
	static void printSelectedEojState(Subnet subnet, String[] eojs) throws EchonetObjectException, InterruptedException, SubnetException{
		RemoteObjectManager remoteManager = new RemoteObjectManager();
		
		InstanceListRequestExecutor executor = new InstanceListRequestExecutor(
			subnet,
			new TransactionManager(subnet),
			remoteManager
		);
		
		//情報取得実行
		executor.execute();
		
		//上のが終わるまで待つ
		executor.join();
		
		//サブネット内の全部のノード取得
		for (Node node : remoteManager.getNodes()) {
			System.out.println("node : " + node);
			//そのノード内の指定したEOJを持つオブジェクトを探してEPC全部出力
			for(String eoj : eojs){
				RemoteObject obj = remoteManager.get(node, new EOJ(eoj));
				printEpcs(obj);
			}
        }
	}

	public static void main(String[] args){
		try{
			Inet4Subnet subnet = Inet4Subnet.startSubnet();
			System.out.println("started subnet");
			printAllEojState(subnet);
        }catch(Exception e){
			System.out.println(e);
		}
	}
}
