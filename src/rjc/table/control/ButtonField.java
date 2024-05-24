/**************************************************************************
 *  Copyright (C) 2024 by Richard Crook                                   *
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

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import rjc.table.view.Colours;

/*************************************************************************************************/
/******* ExpandingField that has spin or drop-down button, numeric value, prefix & suffix ********/
/*************************************************************************************************/

public class ButtonField extends ExpandingField
{
  private double           m_minValue;            // minimum number allowed
  private double           m_maxValue;            // maximum number allowed

  private double           m_page;                // value increment or decrement on page-up or page-down
  private double           m_step;                // value increment or decrement on arrow-up or arrow-down

  private String           m_prefix;              // prefix shown before value
  private String           m_suffix;              // suffix shown after value

  private ButtonType       m_buttonType;          // button type, null means no button
  private Canvas           m_button;              // canvas to show button

  private static final int BUTTONS_WIDTH_MAX = 16;
  private static final int BUTTONS_PADDING   = 2;

  public enum ButtonType
  {
    DOWN, UP_DOWN
  }

  // handler for mouse scroll events to step increase/decrease numeric value
  private final EventHandler<ScrollEvent> scrollHandler = event ->
  {
    event.consume();
    if ( event.getDeltaY() > 0 )
      changeValue( m_step );
    else
      changeValue( m_step );
  };

  /**************************************** constructor ******************************************/
  public ButtonField()
  {
    // set default spin editor characteristics
    setPrefixSuffix( null, null );
    setRange( 0.0, 999.0 );
    setStepPage( 1.0, 10.0 );
    setButtonType( ButtonType.UP_DOWN );

    // react to key presses and button mouse clicks
    setOnKeyPressed( event -> keyPressed( event ) );
    m_button.setOnMousePressed( event -> buttonPressed( event ) );

    // when focused take control of parent's scroll events, otherwise release control
    focusedProperty().addListener( ( observable, oldFocus, newFocus ) ->
    {
      if ( isFocused() )
        getParent().addEventFilter( ScrollEvent.SCROLL, scrollHandler );
      else
        getParent().removeEventFilter( ScrollEvent.SCROLL, scrollHandler );
    } );

  }

  /****************************************** setValue *******************************************/
  public void setValue( Object value )
  {
    // set field text after adding prefix + suffix
    String text = value == null ? "null" : value.toString();
    setText( m_prefix + text + m_suffix );
    positionCaret( m_prefix.length() + text.length() );
  }

  /****************************************** getValue *******************************************/
  public String getValue()
  {
    // return editor text without prefix + suffix
    return getText().substring( m_prefix.length(), getText().length() - m_suffix.length() );
  }

  /***************************************** getDouble *******************************************/
  public double getDouble()
  {
    // attempt to get number from field text, otherwise return min
    try
    {
      return Double.parseDouble( getValue() );
    }
    catch ( Exception exception )
    {
      return m_minValue;
    }
  }

  /****************************************** getPrefix ******************************************/
  public String getPrefix()
  {
    // return prefix
    return m_prefix;
  }

  /****************************************** getSuffix ******************************************/
  public String getSuffix()
  {
    // return suffix
    return m_suffix;
  }

  /*************************************** setPrefixSuffix ***************************************/
  public void setPrefixSuffix( String prefix, String suffix )
  {
    // set prefix and suffix, translating null to ""
    m_prefix = prefix == null ? "" : prefix;
    m_suffix = suffix == null ? "" : suffix;
  }

  /******************************************* getMin ********************************************/
  public double getMin()
  {
    // return minimum allowed spin value
    return m_minValue;
  }

  /******************************************* getMax ********************************************/
  public double getMax()
  {
    // return maximum allowed spin value
    return m_maxValue;
  }

  /******************************************* setRange ******************************************/
  public void setRange( double minValue, double maxValue )
  {
    // check range is valid
    if ( minValue > maxValue )
      throw new IllegalArgumentException( "Min " + minValue + " > Max " + maxValue );

    // set range and reset value to ensure in range
    m_minValue = minValue;
    m_maxValue = maxValue;
  }

  /***************************************** setStepPage *****************************************/
  public void setStepPage( double step, double page )
  {
    // set step and page increment/decrement sizes
    m_step = step;
    m_page = page;
  }

  /**************************************** setButtonType ****************************************/
  public void setButtonType( ButtonType type )
  {
    // set button type for this editor
    m_buttonType = type;
    m_button = new Canvas();
    m_button.setManaged( false );
    m_button.setCursor( Cursor.DEFAULT );
    getChildren().add( m_button );

    // add listeners to (re)draw button every time editor changes size
    heightProperty().addListener( ( property, oldHeight, newHeight ) -> drawButton() );
    widthProperty().addListener( ( property, oldWidth, newWidth ) -> drawButton() );
  }

  /***************************************** drawButton ******************************************/
  private void drawButton()
  {
    // determine size and draw button
    double h = getHeight() - 2 * BUTTONS_PADDING;
    double w = getWidth() / 2;
    if ( w > BUTTONS_WIDTH_MAX )
      w = BUTTONS_WIDTH_MAX;

    // set editor insets and button position
    double pad = getPadding().getLeft();
    setPadding( new Insets( getPadding().getTop(), w + pad, getPadding().getBottom(), pad ) );
    m_button.setLayoutX( getWidth() - w - BUTTONS_PADDING );
    m_button.setLayoutY( BUTTONS_PADDING );

    // if size has not changed, no need to re-draw
    if ( m_button.getHeight() == h && m_button.getWidth() == w )
      return;

    // set size and fill background
    m_button.setHeight( h );
    m_button.setWidth( w );
    GraphicsContext gc = m_button.getGraphicsContext2D();
    gc.setFill( Colours.BUTTON_BACKGROUND );
    gc.fillRect( 0.0, 0.0, w, h );

    // draw correct button depending on button type
    int x1, y1, y2;
    gc.setStroke( Colours.BUTTON_ARROW );
    switch ( m_buttonType )
    {
      case DOWN:
        // draw down arrow
        x1 = (int) ( w * 0.3 + 0.5 );
        y1 = (int) ( h * 0.3 + 0.5 );
        y2 = (int) ( h - y1 );
        for ( int y = y1; y <= y2; y++ )
        {
          double x = x1 + ( w * 0.5 - x1 ) / ( y2 - y1 ) * ( y - y1 );
          gc.strokeLine( x, y + .5, w - x, y + .5 );
        }
        break;

      case UP_DOWN:
        // draw up+down arrows
        x1 = (int) ( w * 0.2 + 0.5 );
        y1 = (int) ( h * 0.1 + 0.6 );
        y2 = (int) ( h * 0.5 - y1 );
        for ( int y = y1; y <= y2; y++ )
        {
          double x = x1 + ( w * 0.5 - x1 ) / ( y2 - y1 ) * ( y2 - y );
          gc.strokeLine( x, y + .5, w - x, y + .5 );
          gc.strokeLine( x, h - ( y + .5 ), w - x, h - ( y + .5 ) );
        }
        break;

      default:
        throw new IllegalArgumentException( "Type=" + m_buttonType );
    }
  }

  /**************************************** buttonPressed ****************************************/
  private void buttonPressed( MouseEvent event )
  {
    // if user clicked top half of buttons, step up, else step down
    requestFocus();
    event.consume();

    if ( event.getY() < m_button.getHeight() / 2 )
      changeValue( m_step );
    else
      changeValue( -m_step );
  }

  /***************************************** keyPressed ******************************************/
  protected void keyPressed( KeyEvent event )
  {
    // action key press to change value up or down
    switch ( event.getCode() )
    {
      case DOWN:
        changeValue( -m_step );
        event.consume();
        break;
      case PAGE_DOWN:
        changeValue( -m_page );
        event.consume();
        break;
      case UP:
        changeValue( m_step );
        event.consume();
        break;
      case PAGE_UP:
        changeValue( m_page );
        event.consume();
        break;
      case HOME:
        setValue( m_minValue );
        event.consume();
        break;
      case END:
        setValue( m_maxValue );
        event.consume();
        break;
      default:
        break;
    }
  }

  /***************************************** changeValue *****************************************/
  private void changeValue( double delta )
  {
    // change spin value by delta overflowing to wrap-field if available
    double num = getDouble() + delta;
    setValue( num );
  }
}
