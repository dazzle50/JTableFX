/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
 *  https://github.com/dazzle50/JTableFX                                  *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.table.control;

import java.util.regex.Pattern;

import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import rjc.table.Utils;
import rjc.table.signal.ObservableStatus;

/*************************************************************************************************/
/************* Enhanced JavaFX TextField that can expand and only allows valid text **************/
/*************************************************************************************************/

/**
 * Text field with optional full-text filtering, shared status reporting, and width expansion.
 * <p>
 * When an allowed-pattern is set, proposed text edits are accepted only if the complete resulting
 * text matches that pattern. When a status object is set, accepted text changes clear the status so
 * the owner can revalidate and report any new problem.
 * <p>
 * Width expansion is enabled by {@link #setWidths(double, double)} and recalculated when width
 * limits, text, or padding changes.
 */
public class ExpandingField extends TextField implements IObservableStatus
{
  private Pattern          m_allowed;  // pattern defining text allowed to be entered
  private double           m_minWidth; // minimum width for editor in pixels
  private double           m_maxWidth; // maximum width for editor in pixels

  private ObservableStatus m_status;   // error status of text field

  /**************************************** constructor ******************************************/
  /**
   * Creates an expanding text field with no input filter and no status object.
   */
  public ExpandingField()
  {
    // listen to text changes or padding changes to check if field width needs changing
    textProperty().addListener( ( property, oldText, newText ) -> checkWidth() );
    paddingProperty().addListener( ( property, oldPadding, newPadding ) -> checkWidth() );
  }

  /***************************************** checkWidth ******************************************/
  private void checkWidth()
  {
    // if min & max width set, increase editor width if needed to show whole text
    if ( m_minWidth > 0.0 && m_maxWidth > m_minWidth )
    {
      Text text = new Text( getText() );
      text.setFont( getFont() );
      double width = text.getLayoutBounds().getWidth() + getPadding().getLeft() + getPadding().getRight() + 2;
      width = Math.ceil( Utils.clamp( width, m_minWidth, m_maxWidth ) );
      if ( getWidth() != width )
      {
        setMinWidth( width );
        setMaxWidth( width );
      }
    }
  }

  /***************************************** replaceText *****************************************/
  /**
   * Replaces text only when the resulting full text is allowed.
   *
   * @param start start index of the replacement
   * @param end   end index of the replacement
   * @param text  replacement text
   */
  @Override
  public void replaceText( int start, int end, String text )
  {
    // only process text updates that result in allowed new text
    String oldText = getText();
    String newText = oldText.substring( 0, start ) + text + oldText.substring( end );

    if ( isAllowed( newText ) )
    {
      if ( m_status != null && !newText.equals( oldText ) )
        m_status.clear(); // assume any errors cleared until re-checked & found
      super.replaceText( start, end, text );
    }
  }

  /****************************************** setAllowed *****************************************/
  /**
   * Sets the regular expression used to accept or reject the complete field text.
   *
   * @param regex regular expression for allowed full text, or null to allow all text
   */
  public void setAllowed( String regex )
  {
    // regular expression that limits what can be entered into editor
    m_allowed = regex == null ? null : Pattern.compile( regex );
  }

  /****************************************** isAllowed ******************************************/
  /**
   * Returns whether the supplied full text is allowed by the current regular expression.
   *
   * @param text proposed complete field text
   * @return true if no regular expression is set or the text matches it
   */
  public boolean isAllowed( String text )
  {
    // return true if text is allowed
    return m_allowed == null || m_allowed.matcher( text ).matches();
  }

  /****************************************** setWidths ******************************************/
  /**
   * Sets the minimum and maximum widths used when expanding to fit the current text.
   * <p>
   * The field width is recalculated immediately.
   *
   * @param min minimum width in pixels
   * @param max maximum width in pixels
   */
  public void setWidths( double min, double max )
  {
    // set editor minimum and maximum width (only used for table cell editors)
    m_minWidth = min;
    m_maxWidth = max;
    checkWidth();
  }

  /***************************************** setStatus *******************************************/
  /**
   * Sets the status object cleared when accepted text changes.
   *
   * @param status shared status object; must not be null
   * @throws NullPointerException if status is null
   */
  @Override
  public void setStatus( ObservableStatus status )
  {
    // set text field status
    if ( status == null )
      throw new NullPointerException( "Status cannot be null" );
    m_status = status;
  }

  /***************************************** getStatus *******************************************/
  /**
   * Returns the status object used by this field.
   *
   * @return status object, or null if none has been set
   */
  @Override
  public ObservableStatus getStatus()
  {
    // return text field status
    return m_status;
  }

  /****************************************** toString *******************************************/
  /**
   * Returns a compact debug representation containing the instance name and current text.
   */
  @Override
  public String toString()
  {
    // return field instance and current text
    return Utils.name( this ) + "[" + getText() + "]";
  }

}