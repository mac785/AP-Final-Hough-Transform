import java.awt.GridLayout;

import javax.swing.JFrame;

public class HoughFrame extends JFrame
{
	public HoughFrame()
	{
		super("Hough");
		setSize(1000,800);
		setResizable(true);
		getContentPane().setLayout(new GridLayout(1,1));
		System.out.println("Starting a new HoughPanel");
		getContentPane().add(new HoughPanel());
		System.out.println("Done with the HoughPannel");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
}
