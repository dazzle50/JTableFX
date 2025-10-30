/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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

package rjc.table.view;

import javafx.geometry.Pos;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;

/*************************************************************************************************/
/*************************** Custom MenuItem with Label and TextField ****************************/
/*************************************************************************************************/

/**
 * A custom menu item that contains a label and text field for user input.
 * The text field can trigger actions when Enter is pressed or menu item is activated.
 * Multiple instances can be aligned using the static align method.
 */
public class MenuItemTextField extends CustomMenuItem
{
  private final TextField  m_textField;
  private final Label      m_label;
  private final static int SPACING = 5;

  /**************************************** constructor ******************************************/
  /**
   * Creates a new labelled text field menu item.
   * 
   * @param labelText the text to display in the label before the text field
   */
  public MenuItemTextField( String labelText )
  {
    // create label and text field components
    m_label = new Label( labelText );
    m_textField = new TextField();

    // layout components in horizontal box with spacing
    var hbox = new HBox( SPACING );
    hbox.getChildren().addAll( m_label, m_textField );
    hbox.setAlignment( Pos.CENTER_LEFT );
    setContent( hbox );
  }

  /**************************************** setOnAction ******************************************/
  /**
   * Sets the action to run when Enter is pressed in the text field or menu item is activated.
   * 
   * @param runnable the action to execute
   */
  public void setOnAction( Runnable runnable )
  {
    // trigger action on text field enter key
    m_textField.setOnAction( e -> runnable.run() );

    // trigger action on menu item activation
    super.setOnAction( e -> runnable.run() );
  }

  /**************************************** getFieldText *****************************************/
  /**
   * Returns the current text entered in the text field.
   * 
   * @return the text field content, or empty string if no text entered
   */
  public String getFieldText()
  {
    return m_textField.getText();
  }

  /******************************************** align ********************************************/
  /**
   * Aligns multiple menu items by setting all labels to the same width (the maximum width).
   * Also sets the text fill colour for all labels.
   * Note: call this method after all menu items have been created and initial layout calculated.
   * 
   * @param textFill the colour to apply to all labels
   * @param items the menu items to align
   */
  public static void align( Paint textFill, MenuItemTextField... items )
  {
    // find maximum label width across all items
    double maxWidth = 0;
    for ( var item : items )
    {
      double width = item.m_label.prefWidth( -1 );
      if ( width > maxWidth )
        maxWidth = width;
    }

    // set all labels to maximum width and apply text fill colour
    for ( var item : items )
    {
      item.m_label.setMinWidth( maxWidth );
      item.m_label.setPrefWidth( maxWidth );
      item.m_label.setTextFill( textFill );
    }
  }
}