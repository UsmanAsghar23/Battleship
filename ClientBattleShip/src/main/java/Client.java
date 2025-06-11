import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;
public class Client extends Thread{
	Socket socketClient;
	ObjectOutputStream out;
	ObjectInputStream in;
	private Consumer<Serializable> callback;
	private List<String> usernames;

	Client(Consumer<Serializable> call){
		callback = call;
	}
	public void run() {

		try {
			socketClient= new Socket("127.0.0.1",5555);
			out = new ObjectOutputStream(socketClient.getOutputStream());
			out.flush();
			in = new ObjectInputStream(socketClient.getInputStream());
			socketClient.setTcpNoDelay(true);
		}

		catch(Exception ignored) {}
		while(true) {
			try {
				//handle incoming messages
				Serializable message = (Serializable) in.readObject();
				callback.accept(message);
			}
			catch(Exception ignored) {}
		}
	}

	public void send(Message data) {
		try {
			out.reset();
			out.writeObject(data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}