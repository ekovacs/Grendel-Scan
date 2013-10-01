/**
 * 
 */
package com.grendelscan.GUI.customControls.basic;

import org.eclipse.swt.graphics.Font;
import com.grendelscan.GUI.customControls.basic.GComposite;
import org.eclipse.swt.widgets.Text;

import com.grendelscan.GUI.GuiUtils;

/**
 * @author dbyrne
 *
 */
public class GText extends Text
{

	/**
	 * @param parent
	 * @param style
	 */
	public GText(GComposite parent, int style)
	{
		super(parent, style);
		setFont(GuiUtils.getFont(0));
	}

	public GText(GGroup parent, int style)
	{
		super(parent, style);
		setFont(GuiUtils.getFont(0));
	}

	public GText(GTable parent, int style)
	{
		super(parent, style);
		setFont(GuiUtils.getFont(0));
	}

	@Override
	public void setFont(Font font)
	{
		super.setFont(font);
	}

}
