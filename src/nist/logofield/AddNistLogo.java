package nist.logofield;

import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AddNistLogo extends JPanel{
	
	public AddNistLogo(int width, int height) {
		super(new FlowLayout(0));
		
		//ImageIcon ii = new ImageIcon(this.getClass().getResource("nistlogo.jpg"));
		BufferedImage image = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = (Graphics2D) image.createGraphics();
		g2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
		//g2d.drawImage(ii.getImage(), 0, 0, width, height, null);
		
		JLabel picLabel = new JLabel(new ImageIcon(image));
		
		this.setToolTipText("<html>www.nist.gov</html>");
		add(picLabel);
	}

}
