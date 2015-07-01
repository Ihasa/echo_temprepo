import echowand.object.*;
import echowand.net.*;
import echowand.logic.*;
import echowand.service.*;
import echowand.common.*;

public class SendData{
	//obj��EOJ�Ƃ��ׂẴv���p�e�B���o��
	static void printEpcs(RemoteObject obj)throws EchonetObjectException{
		//���̃I�u�W�F�N�g�̂��ׂĂ�EPC
        for (int i = 0x80; i <= 0xff; i++) {
        	EPC epc = EPC.fromByte((byte) i);
            if (obj.isGettable(epc)) {
				System.out.println("--" + obj.getEOJ());
            	System.out.println("----" + epc + " " + obj.getData(epc));
            }
        }
	}

	//�T�u�l�b�g���ɂ��邷�ׂẴI�u�W�F�N�g�̂��ׂẴv���p�e�B���o��
	static void printAllEojState(Subnet subnet) throws EchonetObjectException, InterruptedException, SubnetException{
		RemoteObjectManager remoteManager = new RemoteObjectManager();
		
		InstanceListRequestExecutor executor = new InstanceListRequestExecutor(
			subnet,
			new TransactionManager(subnet),
			remoteManager
		);
		
		//���擾���s
		executor.execute();
		System.out.println("executed");
		//��̂��I���܂ő҂�
		executor.join();
		System.out.println("joined");
		System.out.println("" +  remoteManager.getNodes().size());
		//�T�u�l�b�g���̑S���̃m�[�h�擾
		for (Node node : remoteManager.getNodes()) {
			System.out.println("node : " + node);
			//���̃m�[�h���̑S���̃I�u�W�F�N�g
			for(RemoteObject obj : remoteManager.getAtNode(node)){
				printEpcs(obj);
			}
        }
	}
	
	//�����Ŏw�肵��EOJ�����I�u�W�F�N�g�̂��ׂẴv���p�e�B���o��
	static void printSelectedEojState(Subnet subnet, String[] eojs) throws EchonetObjectException, InterruptedException, SubnetException{
		RemoteObjectManager remoteManager = new RemoteObjectManager();
		
		InstanceListRequestExecutor executor = new InstanceListRequestExecutor(
			subnet,
			new TransactionManager(subnet),
			remoteManager
		);
		
		//���擾���s
		executor.execute();
		
		//��̂��I���܂ő҂�
		executor.join();
		
		//�T�u�l�b�g���̑S���̃m�[�h�擾
		for (Node node : remoteManager.getNodes()) {
			System.out.println("node : " + node);
			//���̃m�[�h���̎w�肵��EOJ�����I�u�W�F�N�g��T����EPC�S���o��
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
