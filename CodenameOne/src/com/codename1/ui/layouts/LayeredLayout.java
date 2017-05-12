/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.layouts;

import com.codename1.io.Util;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.LayeredLayout.LayeredLayoutConstraint.Inset;
import com.codename1.ui.plaf.Style;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * The {@code LayeredLayout} places the components in order one on top of the
 * other and sizes them all to the size of the largest component. This is useful
 * when trying to create an overlay on top of an existing component. E.g. an "x"
 * button to allow removing the component as shown here</p>
 *
 * <img src="https://www.codenameone.com/img/developer-guide/layered-layout.png" alt="The X on this button was placed there using the layered layout code below" />
 *
 * <p>
 * The code to generate this UI is slightly complex and contains very little
 * relevant pieces. The only truly relevant piece the last line of code:</p>
 *
 * <script src="https://gist.github.com/codenameone/d0491ce08ce6b889bbd5.js"></script>*
 *
 *
 * <p>
 * We are doing three distinct things here:</p>
 * <ul>
 * .
 * <li> We are adding a layered layout to the form.</li>
 * <li> We are creating a layered layout and placing two components within. This
 * would be the equivalent of just creating a {@code LayeredLaout}
 * {@link com.codename1.ui.Container} and invoking `add` twice.</li>
 * .* <li> We use
 * https://www.codenameone.com/javadoc/com/codename1/ui/layouts/FlowLayout.html[FlowLayout]
 * to position the `X` close button in the right position.</li>
 * </ul>
 *
 * <p>
 * A common use case for {@code LayeredLayout} is the iOS carousel effect which
 * we can achieve by combing the {@code LayeredLayout} with
 * {@link com.codename1.ui.Tabs}.
 * </p>
 * <script src="https://gist.github.com/codenameone/e981c3f91f98f1515987.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-tabs-swipe1.png" alt="Tabs carousel page 1" />
 *
 * <p>
 * Notice that the layered layout sizes all components to the exact same size
 * one on top of the other. It usually requires that we use another container
 * within; in order to position the components correctly.<br />
 *
 * Forms have a built in layered layout that you can access via
 * `getLayeredPane()`, this allows you to overlay elements on top of the content
 * pane.<br />
 *
 * The layered pane is used internally by components such as {@link com.codename1.components.InteractionDialog},
 * {@link com.codename1.u./AutoCompleteTextField} etc.
 * </p>
 * <p>
 * Warning: Placing native widgets within a layered layout is problematic due to
 * the behavior of peer components. Sample of peer components include the
 * {@link com.codename1.ui.BrowserComponent}, video playback etc.
 * </p>
 *
 * @see com.codename1.ui.Form#getLayeredPane()
 * @see com.codename1.ui.Form#getLayeredPane(java.lang.Class, boolean)
 * @see com.codename1.ui.Form#setGlassPane(com.codename1.ui.Painter)
 * @author Shai Almog
 */
public class LayeredLayout extends Layout {

    /**
     * Unit used for insets.  Millimetres.
     * 
     * @see Inset#unit(byte) 
     * @see Inset#changeUnits(byte) 
     */
    public static final byte UNIT_DIPS = Style.UNIT_TYPE_DIPS;
    
    /**
     * Unit used for insets.  Pixels.
     * 
     * @see Inset#unit(byte) 
     * @see Inset#changeUnits(byte) 
     */
    public static final byte UNIT_PIXELS = Style.UNIT_TYPE_PIXELS;
    
    /**
     * Unit used for insets.  Percent.
     * 
     * @see Inset#unit(byte) 
     * @see Inset#changeUnits(byte) 
     */
    public static final byte UNIT_PERCENT = Style.UNIT_TYPE_SCREEN_PERCENTAGE;
    
    /**
     * Unit used for insets.  Auto.  Auto unit type for an inset indicates the 
     * the inset will be automatically determined at layout time.
     * 
     * @see Inset#unit(byte) 
     * @see Inset#changeUnits(byte) 
     */
    public static final byte UNIT_AUTO = 100;

    /**
     * Temp collection to keep track of which components in the container
     * have been laid out.
     */
    private HashSet<Component> tmpLaidOut = new HashSet<Component>();

    @Override
    public void addLayoutComponent(Object value, Component comp, Container c) {
        if (value instanceof LayeredLayoutConstraint.Inset) {
            value = ((LayeredLayoutConstraint.Inset)value).constraint();
        }
        if (value instanceof LayeredLayoutConstraint) {
            
            installConstraint((LayeredLayoutConstraint)value, comp);
        }
    }
    
    /**
     * Wraps {@link #getComponentConstraint(com.codename1.ui.Component) } and casts it
     * directly to {@link LayeredLayoutConstraint}.
     * @param cmp The component whose constraint we want to retrieve.
     * @return The layered layout constraint for this component.
     */
    public LayeredLayoutConstraint getLayeredLayoutConstraint(Component cmp) {
        return (LayeredLayoutConstraint)getComponentConstraint(cmp);
    }
    
    /**
     * Installs the given constraint in the provided component.
     * @param constraint
     * @param cmp
     * @return 
     */
    private LayeredLayoutConstraint installConstraint(LayeredLayoutConstraint constraint, Component cmp) {
        
        if (constraint.outer() != this || (constraint.cmp != null && constraint.cmp != cmp)) {
            LayeredLayoutConstraint tmp = createConstraint();
            constraint.copyTo(tmp);
            constraint = tmp;
        }
        constraint.cmp = cmp;
        cmp.putClientProperty("$$LayeredLayoutConstraint", constraint);
        return constraint;
    }

    /**
     * Makes a copy of the given constraint.
     * @param constraint The constraint to copy.
     * @return The copied constraint.
     */
    @Override
    public Object cloneConstraint(Object constraint) {
        if (constraint instanceof LayeredLayoutConstraint) {
            return ((LayeredLayoutConstraint)constraint).copy();
        }
        return super.cloneConstraint(constraint);
    }

    
   
    /*
    // We don't see to use this right now so commenting out.  However
    // it is conceivable that we might want to reintroduce this ability later, so
    // I'm leaving the code here.
    private void uninstallConstraint(Component cmp) {
        LayeredLayoutConstraint constraint = (LayeredLayoutConstraint)getComponentConstraint(cmp);
        if (constraint != null) {
            constraint.cmp = null;
        }
        cmp.putClientProperty("$$LayeredLayoutConstraint", null);
    }
    */

    /**
     * Gets the LayeredLayoutConstraint associated with the given component.
     * 
     * May return null if there is no constraint.
     * @param comp
     * @return 
     */
    @Override
    public Object getComponentConstraint(Component comp) {
        return comp.getClientProperty("$$LayeredLayoutConstraint");
    }

    /**
     * Creates a default layered layout constraint.  Default constraint
     * has zero insets on all four sides.
     * @param constraint
     * @return 
     */
    public LayeredLayoutConstraint createConstraint(String constraint) {
        return new LayeredLayoutConstraint().setInsets(constraint);
    }
    
    /**
     * If the given component already has a LayeredLayoutConstraint, then this 
     * will return it. Otherwise it will create a constraint, install it in {@literal cmp}
     * and return the constraint for inspection or manipulation.
     * @param cmp The component whose constraint we wish to retrieve.
     * @return The constraint for a given component.
     */
    public LayeredLayoutConstraint getOrCreateConstraint(Component cmp) {
        LayeredLayoutConstraint constraint = (LayeredLayoutConstraint)getComponentConstraint(cmp);
        if (constraint == null) {
            //System.out.println("Constraint is null... creating a new one");
            constraint = createConstraint();
            constraint = installConstraint(constraint, cmp);
        }
        return constraint;
    }
    
    /**
     * Gets an {@link Inset} associated with the provided component
     * @param cmp The component whose inset we wish to retrieve.
     * @param side The side of the inset.  One of {@link Component#TOP}, {@link Component#LEFT}, {@link Component#BOTTOM} 
     * or {@link Component#RIGHT}.
     * @return The {@link Inset} for the given side of the component.
     */
    public Inset getInset(Component cmp, int side) {
        return getOrCreateConstraint(cmp).insets[side];
    }
    
    /**
     * Returns the insets for the given component as a string.  This can return the 
     * insets in one of two formats depending on the value of the {@literal withLabels} 
     * parameter.
     * @param cmp The component whose insets we wish to retrieve.
     * @param withLabels If {@literal false}, then this returns a string of the format {@code "top right bottom left"}
     * e.g {@code "2mm 2mm 2mm 2mm"}.  If {@literal true}, then it will be formatted like CSS properties: {@code "top:2mm; right:2mm; bottom:2mm; left:2mm"}. 
     * @return The insets associated with {@literal cmp} as a string. Each inset will include the unit.   E.g.:
     * <ul><li>{@literal 2mm} = 2 millimetres/dips</li><li>{@literal 2px} = 2 pixels</li><li>{@literal 25%} = 25%</li><li>{@literal auto} = Flexible inset</li></ul>
     * 
     */
    public String getInsetsAsString(Component cmp, boolean withLabels) {
        return getOrCreateConstraint(cmp).getInsetsAsString(withLabels);
    }
    
    /**
     * Gets the top inset as a string. Return value will include the unit, so the following
     * are possible values:
     * <p>
     * <ul><li>{@literal 2mm} = 2 millimetres</li>
     * <li>{@literal 2px} = 2 pixels</li>
     * <li>{@literal 25%} = 25%</li>
     * <li>{@literal auto} = Flexible Inset</li>
     * </ul>
     * </p>
     * @param cmp The component whose inset we wish to retrieve.
     * @return The inset formatted as a string with the unit abbreviation ("mm", "px", or "%") suffixed.
     */
    public String getTopInsetAsString(Component cmp) {
        return getOrCreateConstraint(cmp).top().getValueAsString();
    }
    
    /**
     * Gets the bottom inset as a string. Return value will include the unit, so the following
     * are possible values:
     * <p>
     * <ul><li>{@literal 2mm} = 2 millimetres</li>
     * <li>{@literal 2px} = 2 pixels</li>
     * <li>{@literal 25%} = 25%</li>
     * <li>{@literal auto} = Flexible Inset</li>
     * </ul>
     * </p>
     * @param cmp The component whose inset we wish to retrieve.
     * @return The inset formatted as a string with the unit abbreviation ("mm", "px", or "%") suffixed.
     */
    public String getBottomInsetAsString(Component cmp) {
        return getOrCreateConstraint(cmp).bottom().getValueAsString();
    }
    
    /**
     * Gets the left inset as a string. Return value will include the unit, so the following
     * are possible values:
     * <p>
     * <ul><li>{@literal 2mm} = 2 millimetres</li>
     * <li>{@literal 2px} = 2 pixels</li>
     * <li>{@literal 25%} = 25%</li>
     * <li>{@literal auto} = Flexible Inset</li>
     * </ul>
     * </p>
     * @param cmp The component whose inset we wish to retrieve.
     * @return The inset formatted as a string with the unit abbreviation ("mm", "px", or "%") suffixed.
     */
    public String getLeftInsetAsString(Component cmp) {
        return getOrCreateConstraint(cmp).left().getValueAsString();
    }
    
    /**
     * Gets the right inset as a string. Return value will include the unit, so the following
     * are possible values:
     * <p>
     * <ul><li>{@literal 2mm} = 2 millimetres</li>
     * <li>{@literal 2px} = 2 pixels</li>
     * <li>{@literal 25%} = 25%</li>
     * <li>{@literal auto} = Flexible Inset</li>
     * </ul>
     * </p>
     * @param cmp The component whose inset we wish to retrieve.
     * @return The inset formatted as a string with the unit abbreviation ("mm", "px", or "%") suffixed.
     */
    public String getRightInsetAsString(Component cmp) {
        return getOrCreateConstraint(cmp).right().getValueAsString();
    }
    
    /**
     * Sets the insets for the component {@literal cmp} to the values specified in {@literal insets}.
     * @param cmp The component whose insets we wish to set.
     * @param insets The insets expressed as a string.  See {@link LayeredLayoutConstraint#setInsets(java.lang.String) } for 
     * details on the format of this parameter.
     * @return Self for chaining.
     * @see LayeredLayoutConstraint#setInsets(java.lang.String) For details on the {@literal insets} parameter
     * format.
     */
    public LayeredLayout setInsets(Component cmp, String insets) {
        getOrCreateConstraint(cmp).setInsets(insets);
        return this;
    }
    
    /**
     * Sets the top inset for this component to the prescribed value.
     * @param cmp The component whose inset we wish to set.
     * @param inset The inset value, including unit.  Units are Percent (%), Millimetres (mm), Pixels (px), and "auto".  E.g. the 
     * following insets values would all be acceptable:
     * <p>
     * <ul>
     *   <li>{@code "2mm"} = 2 millimetres</li>
     *   <li>{@code "2px"} = 2 pixels</li>
     *   <li>{@code "25%"} = 25 percent.</li>
     *   <li>{@code "auto"} = Flexible inset</li>
     * </ul>
     * </p>
     * @return Self for chaining.
     */
    public LayeredLayout setInsetTop(Component cmp, String inset) {
        getOrCreateConstraint(cmp).top().setValue(inset);
        return this;
    }
    
    /**
     * Sets the top inset for this component to the prescribed value.
     * @param cmp The component whose inset we wish to set.
     * @param inset The inset value, including unit.  Units are Percent (%), Millimetres (mm), Pixels (px), and "auto".  E.g. the 
     * following insets values would all be acceptable:
     * <p>
     * <ul>
     *   <li>{@code "2mm"} = 2 millimetres</li>
     *   <li>{@code "2px"} = 2 pixels</li>
     *   <li>{@code "25%"} = 25 percent.</li>
     *   <li>{@code "auto"} = Flexible inset</li>
     * </ul>
     * </p>
     * @return Self for chaining.
     */
    public LayeredLayout setInsetBottom(Component cmp, String inset) {
        getOrCreateConstraint(cmp).bottom().setValue(inset);
        return this;
    }
    
    /**
     * Sets the left inset for this component to the prescribed value.
     * @param cmp The component whose inset we wish to set.
     * @param inset The inset value, including unit.  Units are Percent (%), Millimetres (mm), Pixels (px), and "auto".  E.g. the 
     * following insets values would all be acceptable:
     * <p>
     * <ul>
     *   <li>{@code "2mm"} = 2 millimetres</li>
     *   <li>{@code "2px"} = 2 pixels</li>
     *   <li>{@code "25%"} = 25 percent.</li>
     *   <li>{@code "auto"} = Flexible inset</li>
     * </ul>
     * </p>
     * @return Self for chaining.
     */
    public LayeredLayout setInsetLeft(Component cmp, String inset) {
       getOrCreateConstraint(cmp).left().setValue(inset);
       return this;
    }
    
    /**
     * Sets the right inset for this component to the prescribed value.
     * @param cmp The component whose inset we wish to set.
     * @param inset The inset value, including unit.  Units are Percent (%), Millimetres (mm), Pixels (px), and "auto".  E.g. the 
     * following insets values would all be acceptable:
     * <p>
     * <ul>
     *   <li>{@code "2mm"} = 2 millimetres</li>
     *   <li>{@code "2px"} = 2 pixels</li>
     *   <li>{@code "25%"} = 25 percent.</li>
     *   <li>{@code "auto"} = Flexible inset</li>
     * </ul>
     * </p>
     * @return Self for chaining.
     */
    public LayeredLayout setInsetRight(Component cmp, String inset) {
       getOrCreateConstraint(cmp).right().setValue(inset);
       return this;
    }
    
    
    /**
     * Sets the reference components for the insets of {@literal cmp}. See {@link LayeredLayoutConstraint#setReferenceComponents(com.codename1.ui.Component...) }
     * for a full description of the parameters.
     * @param cmp The component whose reference components we wish to check.
     * @param referenceComponents The reference components.  This var arg may contain 1 to 4 values.  See {@link LayeredLayoutConstraint#setReferenceComponents(com.codename1.ui.Component...) } 
     * for a full description.
     * @return Self for chaining.
     */
    public LayeredLayout setReferenceComponents(Component cmp, Component... referenceComponents) {
        getOrCreateConstraint(cmp).setReferenceComponents(referenceComponents);
        return this;
    }
    
    /**
     * Sets the reference components for this component as a string of 1 to 4 component indices separated by spaces. An 
     * index of {@literal -1} indicates no reference for the corresponding inset.  See {@link LayeredLayoutConstraint#setReferenceComponentIndices(com.codename1.ui.Container, java.lang.String) }
     * for a description of the {@literal refs} parameter.
     * @param cmp The component whose references we're setting.
     * @param refs Reference components as a string of component indices in the parent.
     * @return Self for chaining.
     */
    public LayeredLayout setReferenceComponents(Component cmp, String refs) {
        getOrCreateConstraint(cmp).setReferenceComponentIndices(cmp.getParent(), refs);
        return this;
    }
    
    /**
     * Sets the reference component for the top inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to anchor the inset to.
     * @return Self for chaining.
     */
    public LayeredLayout setReferenceComponentTop(Component cmp, Component referenceComponent) {
        getOrCreateConstraint(cmp).top().referenceComponent(referenceComponent);
        return this;
    }
    
    /**
     * Sets the reference component for the bottom inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to anchor the inset to.
     * @return Self for chaining.
     */
    public LayeredLayout setReferenceComponentBottom(Component cmp, Component referenceComponent) {
        getOrCreateConstraint(cmp).bottom().referenceComponent(referenceComponent);
        return this;
    }
    
    /**
     * Sets the reference component for the left inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to anchor the inset to.
     * @return Self for chaining.
     */
    public LayeredLayout setReferenceComponentLeft(Component cmp, Component referenceComponent) {
        getOrCreateConstraint(cmp).left().referenceComponent(referenceComponent);
        return this;
    }
    
    /**
     * Sets the reference component for the right inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to anchor the inset to.
     * @return Self for chaining.
     */
    public LayeredLayout setReferenceComponentRight(Component cmp, Component referenceComponent) {
        getOrCreateConstraint(cmp).top().referenceComponent(referenceComponent);
        return this;
    }
    
    /**
     * Sets the reference positions for reference components.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a description of the parameters.
     * @param cmp The component whose insets we are manipulating.
     * @param referencePositions The reference positions for the reference components. See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a full description of this parameter.
     * @return Self for chaining.
     */
    public LayeredLayout setReferencePositions(Component cmp, float... referencePositions) {
        getOrCreateConstraint(cmp).setReferencePositions(referencePositions);
        return this;
    }
    
    /**
     * Sets the reference positions for reference components.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a description of the parameters.
     * @param cmp The component whose insets we are manipulating.
     * @param positions The reference positions for the reference components. See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a full description of this parameter.
     * @return Self for chaining.
     */
    public LayeredLayout setReferencePositions(Component cmp, String positions) {
        getOrCreateConstraint(cmp).setReferencePositions(positions);
        return this;
    }
    
    /**
     * Sets the top inset reference position.  Only applicable if the top inset has a reference
     * component specified. 
     * @param cmp The component whose insets were are manipulating.
     * @param position The position.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) } for a full
     * description of the possible values here.
     * @return 
     */
    public LayeredLayout setReferencePositionTop(Component cmp, float position) {
        getOrCreateConstraint(cmp).top().referencePosition(position);
        return this;
    }
    
    /**
     * Sets the reference component for the top inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to which the inset should be anchored.
     * @param position The position of the reference anchor.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a full description of reference positions.
     * @return 
     */
    public LayeredLayout setReferenceComponentTop(Component cmp, Component referenceComponent, float position) {
        getOrCreateConstraint(cmp).top().referenceComponent(referenceComponent).referencePosition(position);
        return this;
    }
    
    /**
     * Sets the bottom inset reference position.  Only applicable if the top inset has a reference
     * component specified. 
     * @param cmp The component whose insets were are manipulating.
     * @param position The position.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) } for a full
     * description of the possible values here.
     * @return 
     */
    public LayeredLayout setReferencePositionBottom(Component cmp, float position) {
        getOrCreateConstraint(cmp).bottom().referencePosition(position);
        return this;
    }
    
    /**
     * Sets the reference component for the bottom inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to which the inset should be anchored.
     * @param position The position of the reference anchor.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a full description of reference positions.
     * @return 
     */
    public LayeredLayout setReferenceComponentBottom(Component cmp, Component referenceComponent, float position) {
        getOrCreateConstraint(cmp).bottom().referenceComponent(referenceComponent).referencePosition(position);
        return this;
    }
    
    /**
     * Sets the left inset reference position.  Only applicable if the top inset has a reference
     * component specified. 
     * @param cmp The component whose insets were are manipulating.
     * @param position The position.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) } for a full
     * description of the possible values here.
     * @return 
     */
    public LayeredLayout setReferencePositionLeft(Component cmp, float position) {
        getOrCreateConstraint(cmp).left().referencePosition(position);
        return this;
    }
    
    /**
     * Sets the reference component for the left inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to which the inset should be anchored.
     * @param position The position of the reference anchor.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a full description of reference positions.
     * @return 
     */
    public LayeredLayout setReferenceComponentLeft(Component cmp, Component referenceComponent, float position) {
        getOrCreateConstraint(cmp).left().referenceComponent(referenceComponent).referencePosition(position);
        return this;
    }
    
    /**
     * Sets the right inset reference position.  Only applicable if the top inset has a reference
     * component specified. 
     * @param cmp The component whose insets were are manipulating.
     * @param position The position.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) } for a full
     * description of the possible values here.
     * @return 
     */
    public LayeredLayout setReferencePositionRight(Component cmp, float position) {
        getOrCreateConstraint(cmp).right().referencePosition(position);
        return this;
    }
    
    /**
     * Sets the reference component for the right inset of the given component.
     * @param cmp The component whose insets we are manipulating.
     * @param referenceComponent The component to which the inset should be anchored.
     * @param position The position of the reference anchor.  See {@link LayeredLayoutConstraint#setReferencePositions(float...) }
     * for a full description of reference positions.
     * @return 
     */
    public LayeredLayout setReferenceComponentRight(Component cmp, Component referenceComponent, float position) {
        getOrCreateConstraint(cmp).right().referenceComponent(referenceComponent).referencePosition(position);
        return this;
    }
    /**
     * {@inheritDoc}
     */
    public void layoutContainer(Container parent) {
        Style s = parent.getStyle();
        int top = s.getPaddingTop();
        int bottom = parent.getLayoutHeight() - parent.getBottomGap() - s.getPaddingBottom();
        int left = s.getPaddingLeft(parent.isRTL());
        int right = parent.getLayoutWidth() - parent.getSideGap() - s.getPaddingRight(parent.isRTL());

        int numOfcomponents = parent.getComponentCount();
        tmpLaidOut.clear();
        for (int i = 0; i < numOfcomponents; i++) {
            Component cmp = parent.getComponentAt(i);
            layoutComponent(parent, cmp, top, left, bottom, right);
        }

    }

    /**
     * Lays out the specific component within the container.  This will first lay out any components that it depends on.
     * @param parent The parent container being laid out.
     * @param cmp The component being laid out.
     * @param top 
     * @param left
     * @param bottom
     * @param right 
     */
    private void layoutComponent(Container parent, Component cmp, int top, int left, int bottom, int right) {
        if (tmpLaidOut.contains(cmp)) {
            return;
        }
        tmpLaidOut.add(cmp);
        LayeredLayoutConstraint constraint = (LayeredLayoutConstraint) getComponentConstraint(cmp);
        if (constraint != null) {
            constraint.fixDependencies(parent);
            for (LayeredLayoutConstraint.Inset inset : constraint.insets) {
                if (inset.referenceComponent != null && inset.referenceComponent.getParent() == parent) {
                    layoutComponent(parent, inset.referenceComponent, top, left, bottom, right);
                }
            }
        }

        Style s = cmp.getStyle();
        if (constraint != null) {
            int innerTop = top;
            int innerBottom = bottom;
            //left = 0;
            //right = parent.getLayoutWidth();
            int leftInset = constraint.insets[Component.LEFT].calculate(cmp, innerTop, left, innerBottom, right);
            int rightInset = constraint.insets[Component.RIGHT].calculate(cmp, innerTop, left, innerBottom, right);
            int topInset = constraint.insets[Component.TOP].calculate(cmp, innerTop, left, innerBottom, right);
            int bottomInset = constraint.insets[Component.BOTTOM].calculate(cmp, innerTop, left, innerBottom, right);
            cmp.setX(leftInset + s.getMarginLeft(parent.isRTL()));
            cmp.setY(topInset + s.getMarginTop());
            cmp.setWidth(right - left - s.getHorizontalMargins() - rightInset - leftInset);
            cmp.setHeight(bottom - top - s.getVerticalMargins() - bottomInset - topInset);

        } else {

            int x = left + s.getMarginLeft(parent.isRTL());
            int y = top + s.getMarginTop();
            int w = right - left - s.getHorizontalMargins();
            int h = bottom - top - s.getVerticalMargins();

            cmp.setX(x);
            cmp.setY(y);
            cmp.setWidth(w);
            cmp.setHeight(h);
            //System.out.println("Component laid out "+cmp);
        }
    }

    private void calcPreferredValues(Component cmp) {
        if (tmpLaidOut.contains(cmp)) {
            return;
        }
        tmpLaidOut.add(cmp);
        LayeredLayoutConstraint constraint = (LayeredLayoutConstraint) getComponentConstraint(cmp);
        if (constraint != null) {
            constraint.fixDependencies(cmp.getParent());
            for (LayeredLayoutConstraint.Inset inset : constraint.insets) {
                if (inset.referenceComponent != null && inset.referenceComponent.getParent() == cmp.getParent()) {
                    calcPreferredValues(inset.referenceComponent);
                }
                inset.calcPreferredValue(cmp.getParent(), cmp);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Dimension getPreferredSize(Container parent) {
        int maxWidth = 0;
        int maxHeight = 0;
        int numOfcomponents = parent.getComponentCount();
        tmpLaidOut.clear();
        for (int i = 0; i < numOfcomponents; i++) {
            Component cmp = parent.getComponentAt(i);
            calcPreferredValues(cmp);
            LayeredLayoutConstraint constraint = (LayeredLayoutConstraint) getComponentConstraint(cmp);
            int vInsets = 0;
            int hInsets = 0;
            if (constraint != null) {
                vInsets += constraint.insets[Component.TOP].preferredValue
                        + constraint.insets[Component.BOTTOM].preferredValue;
                hInsets += constraint.insets[Component.LEFT].preferredValue
                        + constraint.insets[Component.RIGHT].preferredValue;
            }
            maxHeight = Math.max(maxHeight, cmp.getPreferredH() + cmp.getStyle().getMarginTop() + cmp.getStyle().getMarginBottom() + vInsets);
            maxWidth = Math.max(maxWidth, cmp.getPreferredW() + cmp.getStyle().getMarginLeftNoRTL() + cmp.getStyle().getMarginRightNoRTL() + hInsets);

        }
        Style s = parent.getStyle();
        Dimension d = new Dimension(maxWidth + s.getPaddingLeftNoRTL() + s.getPaddingRightNoRTL(),
                maxHeight + s.getPaddingTop() + s.getPaddingBottom());
        return d;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "LayeredLayout";
    }

    /**
     * {@inheritDoc}
     */
    public boolean isOverlapSupported() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean obscuresPotential(Container parent) {
        return true;
    }

    /**
     * Shorthand for Container.encloseIn(new LayeredLayout(), cmps);
     *
     * @param cmps the components to add to a new layered layout container
     * @return a newly created layered layout
     */
    public static Container encloseIn(Component... cmps) {
        return Container.encloseIn(new LayeredLayout(), cmps);
    }
    
    /**
     * Creates a new {@link LayeredLayoutConstraint}
     * @return 
     */
    public LayeredLayoutConstraint createConstraint() {
        return new LayeredLayoutConstraint();
    }

    private static int getOuterHeight(Component cmp) {
        Style s = cmp.getStyle();
        return cmp.getHeight() + s.getVerticalMargins();
    }
    
    private static int getOuterPreferredH(Component cmp) {
        Style s = cmp.getStyle();
        return cmp.getPreferredH() + s.getVerticalMargins();
    }
    
    private static int getInnerHeight(Component cmp) {
        Style s = cmp.getStyle();
        return cmp.getHeight() - s.getPaddingTop() - s.getPaddingBottom();
    }
    
    private static int getInnerPreferredH(Component cmp) {
        Style s = cmp.getStyle();
        return cmp.getPreferredH() - s.getPaddingTop() - s.getPaddingBottom();
    } 
    
    private static int getOuterWidth(Component cmp) {
        Style s = cmp.getStyle();
        return cmp.getWidth() + s.getHorizontalMargins();
    }
    
    private static int getOuterPreferredW(Component cmp) {
        Style s = cmp.getStyle();
        return cmp.getPreferredW() + s.getHorizontalMargins();
    }
    
    
    
    private static int getOuterX(Component cmp) {
        return cmp.getX() - cmp.getStyle().getMarginLeftNoRTL();
    }
    
    private static int getInnerX(Component cmp) {
        return cmp.getX() + cmp.getStyle().getPaddingLeftNoRTL();
    }
    
    private static int getOuterY(Component cmp) {
        return cmp.getY() - cmp.getStyle().getMarginTop();
    }
    
    private static int getInnerY(Component cmp) {
        return cmp.getY() + cmp.getStyle().getPaddingTop();
    }
    
    /**
     * A class that encapsulates the insets for a component in layered layout.
     */
    public class LayeredLayoutConstraint {
        
        /**
         * The component that this constraint belongs to.  If you try to add
         * this constraint to a different component, then it will cause a copy
         * to be made rather than using the same component so that constraints
         * to bleed into other components.
         */
        private Component cmp;
        
        /**
         * Gets the insets as a string.
         * @return 
         */
        public String toString() {
            return getInsetsAsString(true);
        }
        
        
        private LayeredLayout outer() {
            return LayeredLayout.this;
        }
        
        /**
         * Recursively fixes all dependencies so that they are contained inside
         * the provided parent.  A dependency is a "referenceComponent".
         * @param parent The parent container within which all dependencies should reside.
         * @return Self for chaining.
         * @see #setReferenceComponents(com.codename1.ui.Component...) 
         */
        public LayeredLayoutConstraint fixDependencies(Container parent) {
            for (Inset inset : insets) {
                inset.fixDependencies(parent);
            }
            return this;
        }
        
        /**
         * Checks to see if this constraint has any circular dependencies.  E.g.
         * Component A has an inset that has Component B as a reference, which has
         * an inset that depends on Component A. 
         * 
         * @param start The start component to check.
         * @return True this forms a circular dependency.
         */
        public boolean hasCircularDependency(Component start) {
            return dependsOn(start);
        }
        
        /**
         * Gets the inset for a particular side.  
         * @param inset One of {@link Component#TOP}, {@link Component#BOTTOM }, {@link Component#LEFT} or
         * {@link Component#RIGHT}.
         * @return The inset.
         */
        public Inset getInset(int inset) {
            return insets[inset];
        }
        
        /**
         * Makes a full copy of this inset. 
         * @return 
         */
        public LayeredLayoutConstraint copy() {
            return copyTo(new LayeredLayoutConstraint());
        }
        
        /**
         * Copies the settings of this constraint into another constraint.
         * @param dest The inset to copy to.
         * @return Self for chaining.
         */
        public LayeredLayoutConstraint copyTo(LayeredLayoutConstraint dest) {
            for (int i=0; i<4; i++) {
                //Inset inset = new Inset(i);
                dest.insets[i] = insets[i].copyTo(dest.insets[i]);
            }
            return dest;
        }
        
        /**
         * Returns a reference box within which insets of the given component are calculated.  If 
         * {@link cmp} has no reference components in any of its insets, then the resulting box will
         * just bee the inner box of the parent (e.g. the parent's inner bounds.
         * @param parent The parent container.
         * @param cmp The component whose reference box we are obtaining.
         * @param box An out parameter.  This will store the bounds of the box.
         * @return The reference box.  (This will be the same object that is passed in the {@literal box} parameter.
         */
        public Rectangle getReferenceBox(Container parent, Component cmp, Rectangle box) {
            Style parentStyle = parent.getStyle();
            //Style cmpStyle = cmp.getStyle();
            
            if (top().getReferenceComponent() == null) {
                box.setY(parentStyle.getPaddingTop());
            } else {
                Component ref = top().getReferenceComponent();
                box.setY((int)(getOuterY(ref) + (top().getReferencePosition() * getOuterHeight(ref))));
            }
            
            if (left().getReferenceComponent() == null) {
                box.setX(parentStyle.getPaddingLeftNoRTL());
            } else {
                Component ref = left().getReferenceComponent();
                box.setX((int)(getOuterX(ref) + (left().getReferencePosition() * getOuterWidth(ref))));
            }
            
            if (right().getReferenceComponent() == null) {
                box.setWidth(parent.getWidth() - box.getX() - parentStyle.getPaddingRightNoRTL() - parent.getSideGap());
            } else {
                Component ref = right().getReferenceComponent();
                int refX = (int)(getOuterX(ref) + getOuterWidth(ref) - (right().getReferencePosition() * getOuterWidth(ref)));
                box.setWidth(refX - box.getX());
            }
            
            if (bottom().getReferenceComponent() == null) {
                box.setHeight(parent.getHeight() - box.getY() - parentStyle.getPaddingBottom() - parent.getBottomGap());
            } else {
                Component ref = bottom().getReferenceComponent();
                int refY = (int)(getOuterY(ref) + getOuterHeight(ref) - (bottom().getReferencePosition() * getOuterHeight(ref)));
                box.setHeight(refY - box.getY());
            }
            return box;
        }
        
        /**
         * Returns a reference box within which insets of the given component are calculated.  If 
         * {@link cmp} has no reference components in any of its insets, then the resulting box will
         * just bee the inner box of the parent (e.g. the parent's inner bounds.
         * @param parent The parent container.
         * @param cmp The component whose reference box we are obtaining.
         * @return The reference box.  
         */
        public Rectangle getReferenceBox(Container parent, Component cmp) {
            return getReferenceBox(parent, cmp, new Rectangle());
        }
        
        /**
         * Shifts the constraint by the specified number of pixels while maintaining the same units.  This is
         * used mainly in the GUI builder to facilitate dragging and resizing of the component.
         * @param x The number of pixels that the insets should be shifted on the x axis.
         * @param y The number of pixels that the insets should be shifted on the y axis.
         * @param preferMM If an inset needs to be switched from flexible to fixed, then this indicates where it will
         * be changed to millimetres or pixels.  {@literal true} for millimetres.
         * @param parent The parent container in which calculations should be performed. 
         * @return Self for chaining.
         * @see #translateMM(float, float, boolean, com.codename1.ui.Container) 
         */
        public LayeredLayoutConstraint translatePixels(int x, int y, boolean preferMM, Container parent) {
            if (y != 0) {
                if (top().isFlexible() && top().autoIsClipped) {
                    top().changeUnits(preferMM ? UNIT_DIPS : UNIT_PIXELS);
                }
                if (bottom().isFlexible() && bottom().autoIsClipped) {
                    bottom().changeUnits(preferMM ? UNIT_DIPS : UNIT_PIXELS);
                }
                if (top().isFlexible() && bottom().isFlexible()) {
                    // Both top and bottom are flexible... we need to make one of these 
                    // fixed
                    if (y > 0) {
                        // we're moving it to toward the bottom, so we'll choose the bottom 
                        // as an anchor point.
                        bottom().translatePixels(-y, preferMM, parent);
                    } else {
                        top().translatePixels(y, preferMM, parent);
                    }
                } else {
                    if (top().isFixed()) {
                        top().translatePixels(y, preferMM, parent);
                    }
                    if (bottom().isFixed()) {
                        bottom().translatePixels(-y, preferMM, parent);
                    }
                }
            }
            if (x != 0) {
                if (left().isFlexible() && left().autoIsClipped) {
                    left().changeUnits(preferMM ? UNIT_DIPS : UNIT_PIXELS);
                    
                }
                if (right().isFlexible() && right().autoIsClipped) {
                    right().changeUnits(preferMM ? UNIT_DIPS : UNIT_PIXELS);
                }
                if (left().isFlexible() && right().isFlexible()) {
                    // Both top and bottom are flexible... we need to make one of these 
                    // fixed
                    if (x > 0) {
                        // we're moving it to toward the bottom, so we'll choose the bottom 
                        // as an anchor point.
                        right().translatePixels(-x, preferMM, parent);
                    } else {
                        left().translatePixels(x, preferMM, parent);
                    }
                } else {
                    if (left().isFixed()) {
                        left().translatePixels(x, preferMM, parent);
                    }
                    if (right().isFixed()) {
                        right().translatePixels(-x, preferMM, parent);
                    }
                }
            }
            return this;
        }
        
        /**
         * Shifts the constraint by the specified number of millimetres while maintaining the same units.  This is
         * used mainly in the GUI builder to facilitate dragging and resizing of the component.
         * @param x The number of pixels that the insets should be shifted on the x axis.
         * @param y The number of pixels that the insets should be shifted on the y axis.
         * @param preferMM If an inset needs to be switched from flexible to fixed, then this indicates where it will
         * be changed to millimetres or pixels.  {@literal true} for millimetres.
         * @param parent The parent container in which calculations should be performed. 
         * @return Self for chaining.
         * @see #translatePixels(int, int, boolean, com.codename1.ui.Container) 
         */
        public LayeredLayoutConstraint translateMM(float x, float y, boolean preferMM, Container parent) {
            return translatePixels(Display.getInstance().convertToPixels(x), Display.getInstance().convertToPixels(y), preferMM, parent);
            /*
            if (y != 0) {
                if (top().isFlexible() && bottom().isFlexible()) {
                    // Both top and bottom are flexible... we need to make one of these 
                    // fixed
                    if (y > 0) {
                        // we're moving it to toward the bottom, so we'll choose the bottom 
                        // as an anchor point.
                        bottom().translateMM(-y, preferMM, parent);
                    } else {
                        top().translateMM(y, preferMM, parent);
                    }
                } else {
                    if (top().isFixed()) {
                        top().translateMM(y, preferMM, parent);
                    }
                    if (bottom().isFixed()) {
                        bottom().translateMM(-y, preferMM, parent);
                    }
                }
            }
            if (x != 0) {
                if (left().isFlexible() && right().isFlexible()) {
                    // Both top and bottom are flexible... we need to make one of these 
                    // fixed
                    if (x > 0) {
                        // we're moving it to toward the bottom, so we'll choose the bottom 
                        // as an anchor point.
                        right().translateMM(-x, preferMM, parent);
                    } else {
                        left().translateMM(x, preferMM, parent);
                    }
                } else {
                    if (left().isFixed()) {
                        left().translateMM(x, preferMM, parent);
                    }
                    if (right().isFixed()) {
                        right().translateMM(-x, preferMM, parent);
                    }
                }
            }
            return this;
            */
        }
        
        /**
         * Gets the set of insets on this constraint that are fixed.  An inset is 
         * considered fixed if it's unit is NOT {@link #UNIT_AUTO}.
         * @return 
         */
        public Collection<Inset> getFixedInsets() {
            ArrayList<Inset> out = new ArrayList<Inset>();
            for (Inset i : insets) {
                if (i.unit != UNIT_AUTO) {
                    out.add(i);
                }
            }
            return out;
        }
        
        /**
         * Gets the set of insets in this constraint that are flexible.  An inset is 
         * considered flexible if it's unit is {@link #UNIT_AUTO}.
         * @return 
         */
        public Collection<Inset> getFlexibleInsets() {
            ArrayList<Inset> out = new ArrayList<Inset>();
            for (Inset i : insets) {
                if (i.unit == UNIT_AUTO) {
                    out.add(i);
                }
            }
            return out;
        }
        
        /**
         * Gets the reference positions of this constraint as a string.
         * @param withLabels True to return the string in CSS format:  e.g. {@code "top:1.0; right:0; bottom:1.0; left:1.0"}  {@literal false}
         * to return as a space-delimited string of inset reference positions in the order "top right bottom left".  E.g. {@literal "1.0 0 1.0 1.0"}
         * @return The reference positions as a string.
         */
        public String getReferencePositionsAsString(boolean withLabels) {
            StringBuilder sb = new StringBuilder();
            if (withLabels) {
                sb.append("top:").append(top().referencePosition).append("; ")
                    .append("right:").append(right().referencePosition).append("; ")
                    .append("bottom:").append(bottom().referencePosition).append("; ")
                    .append("left:").append(left().referencePosition);
            } else {
                sb.append(top().referencePosition).append(" ")
                        .append(right().referencePosition).append(" ")
                        .append(bottom().referencePosition).append(" ")
                        .append(left().referencePosition);
            }
            return sb.toString();
        }
        
        /**
         * Sets the reference component positions for this constraint from a string.  The string format 
         * may be either using labels following the same output format of {@literal getReferencePositionsAsString(true)}
         * or as a space-delimited string (e.g. {@literal getReferencePositionsAsString(false)}.  When using the label
         * format, you may provide one or more inset values in the string.  E.g. the following are all acceptable:
         * <p>
         * <ul><li>{@literal top:1.0; left:0; right:0; bottom:1.0}</li>
         * <li>{@literal left:0.5}</li>
         * <li>{@literal left:1.0; right:0.5}</li>
         * </ul>
         * </p>
         * <p>
         * If you provide the positions as a space-delimited string, then they are expected to follow the same format
         * as is used in CSS for providing <a href="https://developer.mozilla.org/en/docs/Web/CSS/margin">margin</a>. To summarize:
         * </p>
         * <p>
         * {@code 
//Apply to all four sides 
1.0

//vertical | horizontal
1.0 0

// top | horizontal | bottom 
1.0 0.0 0.5

// top | right | bottom | left 
1.0 1.0 1.0 1.0
}
         * </p>
         * 
         * <p><strong>Interpretation of Reference Positions:</strong></p>
         * <p>When an inset includes a reference component, that means that the inset is "anchored" to that
         * reference component.  I.e. An inset of {@literal 1mm} is measured 1mm from the outer edge of the
         * reference component.  By default it chooses the edge of on the <em>same side</em> as the inset.  So
         * if this is a "left" inset, then it will measure against the "left" outer edge of the reference component.
         * This is the meaning of a {@literal 0} value for the associated reference positions.</p>
         * 
         * <p>A reference position of {@literal 1.0} will start measuring from the opposite edge.  So for a "left" inset,
         * it will measure from the "right" outer edge of the reference component.  You can choose any real value for the
         * reference position, and it will cause the measurement to be scaled accordingly.  E.g. {@literal 0.5.} would measure
         * from the center point of the reference component.</p>
         * @param positionsStr The reference positions.
         * @return Self for chaining.
         */
        public LayeredLayoutConstraint setReferencePositions(String positionsStr) {
            positionsStr = positionsStr.trim();
            LayeredLayoutConstraint cnst = this;
            if (positionsStr.indexOf(":") != -1) {
                String[] parts = Util.split(positionsStr, ";");
                for (String part : parts) {
                    if (part.trim().length() == 0) {
                        continue;
                    }
                    String[] kv = Util.split(part, ":");
                    String key = kv[0].trim();
                    String val = kv[1].trim();
                    if ("top".equals(key)) {
                        cnst.top().referencePosition = Float.parseFloat(val);
                    } else if ("bottom".equals(key)) {
                        cnst.bottom().referencePosition = Float.parseFloat(val);
                    } else if ("left".equals(key)) {
                        cnst.left().referencePosition = Float.parseFloat(val);
                    } else if ("right".equals(key)) {
                        cnst.right().referencePosition = Float.parseFloat(val);
                    }


                }
            } else {
                String[] parts = Util.split(positionsStr, " ");
                if (parts.length == 1) {
                    float f = Float.parseFloat(parts[0]);
                    top().referencePosition = f;
                    right().referencePosition = f;
                    bottom().referencePosition = f;
                    left().referencePosition = f;
                    
                } else if (parts.length == 2) {
                    float f0 = Float.parseFloat(parts[0]);
                    float f1 = Float.parseFloat(parts[1]);
                    top().referencePosition = f0;
                    bottom().referencePosition = f0;
                    left().referencePosition = f1;
                    right().referencePosition = f1;
                } else if (parts.length == 3) {
                    float f0 = Float.parseFloat(parts[0]);
                    float f1 = Float.parseFloat(parts[1]);
                    float f2 = Float.parseFloat(parts[2]);
                    top().referencePosition = f0;
                    left().referencePosition = f1;
                    right().referencePosition = f1;
                    bottom().referencePosition = f2;
                } else if (parts.length == 4) {
                    float f0 = Float.parseFloat(parts[0]);
                    float f1 = Float.parseFloat(parts[1]);
                    float f2 = Float.parseFloat(parts[2]);
                    float f3 = Float.parseFloat(parts[3]);
                    top().referencePosition = f0;
                    right().referencePosition = f1;
                    bottom().referencePosition = f2;
                    left().referencePosition = f3;
                }
            }
            return this;
        }
        
        /**
         * Gets the reference component indexes within the provided {@literal parent} container as a string.
         * If an inset doesn't have a reference component, then the corresponding index will be {@literal -1}.
         * <p>
         * Use the {@literal withLabels} parameter to choose whether to include labels with the indices or not.  E.g:
         * 
         * {@code
         * String indices = getReferenceComponentIndicesAsString(parent, true);
         * // Would return something like
         * // "top:-1; right:2; bottom:-1; left: 0"
         * 
         * indices = getReferenceComponentIndicesAsString(parent, false);
         * // Would return something like:
         * // "-1 2 -1 0"  (i.e. Top Right Bottom Left)
         * 
         * // Interpretation: 
         * //   Top inset has no reference component
         * //   Right inset has component with index 2 (i.e. parent.getComponentIndex(rightReferenceComponent) == 2)
         * //   Bottom inset has no reference component
         * //   Left inset has component with index 0 as a reference component.
         * }
         * </p>
         * @param parent
         * @param withLabels
         * @return 
         */
        public String getReferenceComponentIndicesAsString(Container parent, boolean withLabels) {
            fixDependencies(parent);
            StringBuilder sb = new StringBuilder();
            if (withLabels) {
                if (top().referenceComponent != null && top().referenceComponent.getParent() != null) {
                    Component cmp = top().referenceComponent;
                    sb.append("top:").append(cmp.getParent().getComponentIndex(cmp)).append("; ");
                } else {
                    sb.append("top:-1; ");
                }
                if (right().referenceComponent != null && right().referenceComponent.getParent() != null) {
                    Component cmp = right().referenceComponent;
                    sb.append("right:").append(cmp.getParent().getComponentIndex(cmp)).append("; ");
                } else {
                    sb.append("right:-1; ");
                }
                if (bottom().referenceComponent != null && bottom().referenceComponent.getParent() != null) {
                    Component cmp = bottom().referenceComponent;
                    sb.append("bottom:").append(cmp.getParent().getComponentIndex(cmp)).append("; ");
                } else {
                    sb.append("bottom:-1; ");
                }

                if (left().referenceComponent != null && left().referenceComponent.getParent() != null) {
                    Component cmp = left().referenceComponent;
                    sb.append("left:").append(cmp.getParent().getComponentIndex(cmp)).append("; ");
                } else {
                    sb.append("left:-1");
                }
            } else {
                if (top().referenceComponent != null && top().referenceComponent.getParent() != null) {
                    Component cmp = top().referenceComponent;
                    sb.append(cmp.getParent().getComponentIndex(cmp)).append(" ");
                } else {
                    sb.append("-1 ");
                }
                if (right().referenceComponent != null && right().referenceComponent.getParent() != null) {
                    Component cmp = right().referenceComponent;
                    sb.append(cmp.getParent().getComponentIndex(cmp)).append(" ");
                } else {
                    sb.append("-1 ");
                }
                if (bottom().referenceComponent != null && bottom().referenceComponent.getParent() != null) {
                    Component cmp = bottom().referenceComponent;
                    sb.append(cmp.getParent().getComponentIndex(cmp)).append(" ");
                } else {
                    sb.append("-1 ");
                }

                if (left().referenceComponent != null && left().referenceComponent.getParent() != null) {
                    Component cmp = left().referenceComponent;
                    sb.append(cmp.getParent().getComponentIndex(cmp)).append(" ");
                } else {
                    sb.append("-1");
                }
            }
            
            return sb.toString();
            
        }
        
        /**
         * Sets the reference components of the insets of this constraint as indices of the provided parent
         * container.
         * @param parent The parent container whose children are to be used as reference components.
         * @param indices The indices to set as the reference components.
         * <p>The string format 
         * may be either using labels following the same output format of {@literal cnst.getReferenceComponentIndicesAsString(true)}
         * or as a space-delimited string (e.g. {@literal cnst.getReferenceComponentIndicesAsString(false)}.  When using the label
         * format, you may provide one or more inset values in the string.  E.g. the following are all acceptable:</p>
         * <p>
         * <ul><li>{@literal top:-1; left:0; right:0; bottom:1}</li>
         * <li>{@literal left:1}</li>
         * <li>{@literal left:10; right:-1}</li>
         * </ul>
         * </p>
         * <p>
         * If you provide the positions as a space-delimited string, then they are expected to follow the same format
         * as is used in CSS for providing <a href="https://developer.mozilla.org/en/docs/Web/CSS/margin">margin</a>. To summarize:
         * </p>
         * <p>
         * {@code 
//Set component at index 0 as reference for all 4 insets.
0

//vertical insets use component index 2 | horizontal insets use component index 1
2 1

// top | horizontal | bottom 
-1 3 10

// top | right | bottom | left 
-1 -1 -1 -1
}
*           
         * </p>
         * <p><strong>Note: An index of {@literal -1} means that the corresponding inset has no reference component.</strong></p>
         * @return 
         */
        public LayeredLayoutConstraint setReferenceComponentIndices(Container parent, String indices) {
            indices = indices.trim();
            LayeredLayoutConstraint cnst = this;
            if (indices.indexOf(":") != -1) {
                String[] parts = Util.split(indices, ";");
                for (String part : parts) {
                    if (part.trim().length() == 0) {
                        continue;
                    }
                    String[] kv = Util.split(part, ":");
                    String key = kv[0].trim();
                    String val = kv[1].trim();
                    if ("top".equals(key)) {
                        int index = Integer.parseInt(val);
                        if (index >= 0) {
                            cnst.top().referenceComponent = parent.getComponentAt(index);
                        } else {
                            cnst.top().referenceComponent = null;
                        }
                    } else if ("bottom".equals(key)) {
                        int index = Integer.parseInt(val);
                        if (index >= 0) {
                            cnst.bottom().referenceComponent = parent.getComponentAt(index);
                        } else {
                            cnst.bottom().referenceComponent = null;
                        }
                    } else if ("left".equals(key)) {
                        int index = Integer.parseInt(val);
                        if (index >= 0) {
                            cnst.left().referenceComponent = parent.getComponentAt(index);
                        } else {
                            cnst.left().referenceComponent = null;
                        }
                    } else if ("right".equals(key)) {
                        int index = Integer.parseInt(val);
                        if (index >= 0) {
                            cnst.right().referenceComponent = parent.getComponentAt(index);
                        } else {
                            cnst.right().referenceComponent = null;
                        }
                    }


                }
            } else {
                String[] parts = Util.split(indices, " ");
                if (parts.length == 1) {
                    int i0 = Integer.parseInt(parts[0]);
                    if (i0 == -1) {
                        top().referenceComponent = null;
                        right().referenceComponent = null;
                        bottom().referenceComponent = null;
                        left().referenceComponent = null;
                    } else {
                        Component cmp = parent.getComponentAt(i0);
                        top().referenceComponent = cmp;
                        right().referenceComponent = cmp;
                        bottom().referenceComponent = cmp;
                        left().referenceComponent = cmp;
                    }
                } else if (parts.length == 2) {
                    int i0 = Integer.parseInt(parts[0]);
                    int i1 = Integer.parseInt(parts[1]);
                    Component cmp = null;
                    if (i0 != -1) {
                        cmp = parent.getComponentAt(i0);
                    }
                    top().referenceComponent = cmp;
                    bottom().referenceComponent = cmp;
                    
                    cmp = null;
                    if (i1 != -1) {
                        cmp = parent.getComponentAt(i1);
                    }
                    left().referenceComponent = cmp;
                    right().referenceComponent = cmp;
                } else if (parts.length == 3) {
                    int i0 = Integer.parseInt(parts[0]);
                    int i1 = Integer.parseInt(parts[1]);
                    int i2 = Integer.parseInt(parts[2]);
                    Component cmp = null;
                    if (i0 != -1) {
                        cmp = parent.getComponentAt(i0);
                    }
                    top().referenceComponent = cmp;
                    cmp = null;
                    if (i1 != -1) {
                        cmp = parent.getComponentAt(i1);
                    }
                    left().referenceComponent = cmp;
                    right().referenceComponent = cmp;
                    cmp = null;
                    if (i2 != -1) {
                        cmp = parent.getComponentAt(i2);
                    }
                    bottom().referenceComponent = cmp;
                    
                } else if (parts.length == 4) {
                    int i0 = Integer.parseInt(parts[0]);
                    int i1 = Integer.parseInt(parts[1]);
                    int i2 = Integer.parseInt(parts[2]);
                    int i3 = Integer.parseInt(parts[3]);
                    Component cmp = null;
                    if (i0 != -1) {
                        cmp = parent.getComponentAt(i0);
                    }
                    top().referenceComponent = cmp;
                    cmp = null;
                    if (i1 != -1) {
                        cmp = parent.getComponentAt(i1);
                    }
                    right().referenceComponent = cmp;
                    cmp = null;
                    if (i2 != -1) {
                        cmp = parent.getComponentAt(i2);
                    }
                    
                    bottom().referenceComponent = cmp;
                    cmp = null;
                    if (i3 != -1) {
                        cmp = parent.getComponentAt(i3);
                    }
                    left().referenceComponent = cmp;
                }
            }
            return this;
        }
        
        /**
         * Gets the insets of this constraint as a string. If {@literal withLabels} is {@literal true}, then it
         * will return a string of the format:
         * <p>{@literal top:2mm; right:0; bottom:10%; left:auto}</p>
         * <p>If {@literal withLabels} is {@literal false} then it will return a space-delimited string with 
         * the inset values ordered "top right bottom left" (the same as for CSS margins) order.</p>
         * 
         * @param withLabels
         * @return 
         */
        public String getInsetsAsString(boolean withLabels) {
            StringBuilder sb = new StringBuilder();
            if (withLabels) {
                sb.append("top:").append(top().getValueAsString()).append("; ")
                    .append("right:").append(right().getValueAsString()).append("; ")
                    .append("bottom:").append(bottom().getValueAsString()).append("; ")
                    .append("left:").append(left().getValueAsString());
            } else {
                sb.append(top().getValueAsString()).append(" ")
                        .append(right().getValueAsString()).append(" ")
                        .append(bottom().getValueAsString()).append(" ")
                        .append(left().getValueAsString());
            }
            return sb.toString();
        }
        
        /**
         * Sets the reference components for the constraint.
         * @param refs May contain 1, 2, 3, or 4 values.  If only 1 value is passed, then it is
         * set on all 4 insets.  If two values are passed, then the first is set on the top and bottom
         * insets, and the 2nd is set on the left and right insets (i.e. vertical | horizontal).
         * If 3 values are passed, then, they are used for top, horizontal, and bottom.
         * If 4 values are passed, then they are used for top, right, bottom, left (in that order).
         * @return Self for chaining.
         */
        public LayeredLayoutConstraint setReferenceComponents(Component... refs) {
            if (refs.length == 1) {
                top().referenceComponent = refs[0];
                right().referenceComponent = refs[0];
                bottom().referenceComponent = refs[0];
                left().referenceComponent = refs[0];
            } else if (refs.length == 2) {
                top().referenceComponent = refs[0];
                bottom().referenceComponent = refs[0];
                left().referenceComponent = refs[1];
                right().referenceComponent = refs[1];
            } else if (refs.length == 3) {
                top().referenceComponent = refs[0];
                left().referenceComponent = refs[1];
                right().referenceComponent = refs[1];
                bottom().referenceComponent = refs[2];
            } else if (refs.length == 4) {
                top().referenceComponent = refs[0];
                right().referenceComponent = refs[1];
                bottom().referenceComponent = refs[2];
                left().referenceComponent = refs[3];
            }
            return this;
        }
        
        
        /**
         * Sets the reference positions for the constraint.
         * <p><strong>Interpretation of Reference Positions:</strong></p>
         * <p>When an inset includes a reference component, that means that the inset is "anchored" to that
         * reference component.  I.e. An inset of {@literal 1mm} is measured 1mm from the outer edge of the
         * reference component.  By default it chooses the edge of on the <em>same side</em> as the inset.  So
         * if this is a "left" inset, then it will measure against the "left" outer edge of the reference component.
         * This is the meaning of a {@literal 0} value for the associated reference positions.</p>
         * 
         * <p>A reference position of {@literal 1.0} will start measuring from the opposite edge.  So for a "left" inset,
         * it will measure from the "right" outer edge of the reference component.  You can choose any real value for the
         * reference position, and it will cause the measurement to be scaled accordingly.  E.g. {@literal 0.5.} would measure
         * from the center point of the reference component.</p>
         * 
         * @param p May contain 1, 2, 3, or 4 values.  If only 1 value is passed, then it is
         * set on all 4 insets.  If two values are passed, then the first is set on the top and bottom
         * insets, and the 2nd is set on the left and right insets (i.e. vertical | horizontal).
         * If 3 values are passed, then, they are used for top, horizontal, and bottom.
         * If 4 values are passed, then they are used for top, right, bottom, left (in that order).
         * @return Self for chaining.
         */
        public LayeredLayoutConstraint setReferencePositions(float... p) {
            if (p.length == 1) {
                for (Inset i : insets) {
                    i.referencePosition = p[0];
                }
            } else if (p.length == 2) {
                for (Inset i : insets) {
                    switch (i.side) {
                        case Component.TOP:
                        case Component.BOTTOM:
                            i.referencePosition = p[0];
                            break;
                        default:
                            i.referencePosition = p[1];
                    }
                }
            } else if (p.length == 3) {
                for (Inset i : insets) {
                    switch (i.side) {
                        case Component.TOP:
                            i.referencePosition = p[0];
                            break;
                        case Component.LEFT:
                        case Component.RIGHT:
                            i.referencePosition = p[1];
                            break;
                        default:
                            i.referencePosition = p[2];
                    }
                }
            } else if (p.length == 4) {
                for (Inset i : insets) {
                    switch (i.side) {
                        case Component.TOP:
                            i.referencePosition = p[0];
                            break;
                        case Component.RIGHT:
                            i.referencePosition = p[1];
                            break;
                        case Component.BOTTOM:
                            i.referencePosition = p[2];
                            break;
                        
                        
                        default:
                            i.referencePosition = p[3];
                    }
                }
            }
            return this;
        }
        
        /**
         * Sets the insets for this constraint as a string.  The string may include labels
         * or it may be a space delimited string of values with "top right bottom left" order.
         * 
         * <p>
         * If providing as a space-delimited string of inset values, then you can provide 1, 2, 3, or 4
         * values.  If only 1 value is passed, then it is
         * set on all 4 insets.  If two values are passed, then the first is set on the top and bottom
         * insets, and the 2nd is set on the left and right insets (i.e. vertical | horizontal).
         * If 3 values are passed, then, they are used for top, horizontal, and bottom.
         * If 4 values are passed, then they are used for top, right, bottom, left (in that order).
         * </p>
         * <p>
         * <strong>Example Inputs</strong>
         * </p>
         * <p>
         * <ul>
         *   <li>{@literal "0 0 0 0"} = all 4 insets are zero pixels</li>
         *   <li>{@literal "0 1mm"} = Vertical insets are zero.  Horizontal insets are 1mm</li>
         *   <li>{@literal "10% auto 20%"} = Top inset is 10%.  Horizontal insets are flexible.  Bottom is 20%</li>
         *   <li>{@literal "1mm 2mm 3mm 4mm"} = Top=1mm, Right=2mm, Bottom=3mm, Left=4mm</li>
         * </ul>
         * </p>
         * @param insetStr
         * @return Self for chaining.
         */
        public LayeredLayoutConstraint setInsets(String insetStr) {
            
            LayeredLayoutConstraint cnst = this;
            if (insetStr.indexOf(":") != -1) {
                String[] parts = Util.split(insetStr, ";");
                for (String part : parts) {
                    if (part.trim().length() == 0) {
                        continue;
                    }
                    String[] kv = Util.split(part, ":");
                    String key = kv[0].trim();
                    String val = kv[1].trim();
                    if ("top".equals(key)) {
                        cnst.top().setValue(val);
                    } else if ("bottom".equals(key)) {
                        cnst.bottom().setValue(val);
                    } else if ("left".equals(key)) {
                        cnst.left().setValue(val);
                    } else if ("right".equals(key)) {
                        cnst.right().setValue(val);
                    }


                }
            } else {
                String[] parts = Util.split(insetStr, " ");
                if (parts.length == 1) {
                    top().setValue(parts[0]);
                    right().setValue(parts[0]);
                    bottom().setValue(parts[0]);
                    left().setValue(parts[0]);
                    
                } else if (parts.length == 2) {
                    top().setValue(parts[0]);
                    bottom().setValue(parts[0]);
                    left().setValue(parts[1]);
                    right().setValue(parts[1]);
                } else if (parts.length == 3) {
                    top().setValue(parts[0]);
                    left().setValue(parts[1]);
                    right().setValue(parts[1]);
                    bottom().setValue(parts[2]);
                } else if (parts.length == 4) {
                    top().setValue(parts[0]);
                    right().setValue(parts[1]);
                    bottom().setValue(parts[2]);
                    left().setValue(parts[3]);
                }
            }
            return this;
        }
        
        /**
         * Gets the left inset.
         * @return The left inset
         */
        public Inset left() {
            return insets[Component.LEFT];
        }
        
        /**
         * Gets the right inset.
         * @return The right inset.
         */
        public Inset right() {
            return insets[Component.RIGHT];
        }
        
        /**
         * Gets the top inset
         * @return The top inset
         */
        public Inset top() {
            return insets[Component.TOP];
        }
        
        /**
         * Gets the bottom inset.
         * @return The bottom inset
         */
        public Inset bottom() {
            return insets[Component.BOTTOM];
        }
        
        /**
         * Gets the constraint itself.
         * @return 
         */
        public LayeredLayoutConstraint constraint() {
            return this;
        }
        
        /**
         * The insets for this constraint.
         */
        private final Inset[] insets = new Inset[]{
            new Inset(Component.TOP),
            new Inset(Component.LEFT),
            new Inset(Component.BOTTOM),
            new Inset(Component.RIGHT)
        };

        //private Rectangle preferredBounds;

        /**
         * Gets the dependencies (i.e. recursively gets  all reference components).
         * @param deps A set to add the dependencies to. (An "out" parameter).
         * @return The set of dependencies.  Same as {@literal dep} parameter.
         */
        public Set<Component> getDependencies(Set<Component> deps) {
            for (Inset inset : insets) {
                inset.getDependencies(deps);
            }
            return deps;
        }
        
        /**
         * Gets the dependencies (i.e. recursively gets  all reference components).
         * @return The set of dependencies. 
         */
        public Set<Component> getDependencies() {
            return getDependencies(new HashSet<Component>());
        }
        
        /**
         * Checks to see if this constraint has the given component in its set of dependencies.
         * @param cmp The component to check.
         * @return True if {@literal cmp} is a reference component of some inset in this
         * constraint (recursively).
         */
        public boolean dependsOn(Component cmp) {
            return getDependencies().contains(cmp);
        }

        /**
         * Encapsulates an inset.
         */
        public class Inset {

            
            /**
             * Creates a new inset for the given side.
             * @param side One of {@link Component#TOP}, {@link Component#BOTTOM}, {@link Component#LEFT}, or {@link Component#RIGHT}.
             */
            public Inset(int side) {
                this.side = side;
            }

            /**
             * Prints this inset as a string.
             * @return 
             */
            public String toString() {
                switch (side) {
                    case Component.TOP : return "top="+getValueAsString();
                    case Component.BOTTOM: return "bottom="+getValueAsString();
                    case Component.LEFT: return "left="+getValueAsString();
                    default: return "right="+getValueAsString();
                }
            }
            
            /**
             * Gets the value of this inset as a string. Values will be in the format {@literal <value><unit>}, e.g.
             * {@literal 2mm}, {@literal 15%}, {@literal 5px}, {@literal auto} (meaning it is flexible.
             * @return The value of this inset as a string.
             */
            public String getValueAsString() {
                switch (unit) {
                    case UNIT_DIPS: return value +"mm";
                    case UNIT_PIXELS: return ((int)value)+"px";
                    case UNIT_PERCENT: return value + "%";
                    case UNIT_AUTO: return "auto";
                }
                return null;
            }
            
            /**
             * Fixes dependencies in this inset recursively so that all reference
             * components are children of the given parent container.  If a reference
             * component is not in the parent, then it will first check to find a
             * child of {@literal parent} with the same name as the reference component. 
             * Failing that, it will try to find a child of {@literal parent} with the 
             * same index. 
             * 
             * If an appropriate match is found, it will replace the referenceComponent
             * with the match.
             * 
             * 
             * @param parent The container in which all reference components should reside.
             * @return Self for chaining.
             */
            private Inset fixDependencies(Container parent) {
                Container refParent;
                if (referenceComponent != null && (refParent = referenceComponent.getParent()) != parent) {
                    // The reference component is not in this parent
                    String name = referenceComponent.getName();
                    boolean found = false;
                    if (name != null && name.length() > 0) {
                        for (Component child : parent) {
                            if (name.equals(child.getName())) {
                                referenceComponent = child;
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found && refParent != null) {
                        int index = refParent.getComponentIndex(referenceComponent);
                        if (parent.getComponentCount() > index) {
                            referenceComponent = parent.getComponentAt(index);
                            found = true;
                        }
                    }
                    
                    if (found) {
                        LayeredLayoutConstraint refCnst = getOrCreateConstraint(referenceComponent);
                        refCnst.getInset(side).fixDependencies(parent);
                    }
                    
                }
                return this;
            }
            
            public Inset setValueAsString(String value) {
                setValue(value);
                return this;
            }
            
            public Inset left() {
                return constraint().left();
            }
            
            public Inset right() {
                return constraint().right();
            }
            
            public Inset top() {
                return constraint().top();
            }
            
            public Inset bottom() {
                return constraint().bottom();
            }
            
            public LayeredLayoutConstraint constraint() {
                return LayeredLayoutConstraint.this;
            }
            
            public Inset unit(byte unit) {
                this.unit = unit;
                return this;
            }
            
            public Inset setAuto() {
                return unit(UNIT_AUTO);
            }
            
            public Inset setDips() {
                return unit(UNIT_DIPS);
            }
            
            public Inset setPercent() {
                return unit(UNIT_PERCENT);
            }
            
            public Inset setPixels() {
                return unit(UNIT_PIXELS);
            }
            
            public Inset setPixels(int px) {
                this.value = px;
                return unit(UNIT_PIXELS);
            }
            
            public Inset setDips(float dips) {
                this.value = dips;
                return unit(UNIT_DIPS);
            }
            
            public Inset setPercent(float percent) {
                this.value = percent;
                return unit(UNIT_PERCENT);
            }
            
            public Inset referenceComponent(Component cmp) {
                referenceComponent = cmp;
                return this;
            }
            
            public Inset referencePosition(float position) {
                this.referencePosition = position;
                return this;
            }
            
            public Inset value(float value) {
                this.value = value;
                return this;
            }
            
            public int getSide() {
                return side;
            }
            
            public Component getReferenceComponent() {
                return referenceComponent;
            }
            
            public float getReferencePosition() {
                return referencePosition;
            }
            
            
            
            
            /**
             * One of
             * {@link Component#TOP}, {@link Component#Bottom}, {@link Component#LEFT}, {@link Component#RIGHT}
             */
            private int side;

            /**
             * The component that is used a reference for this inset.
             * {@literal null} for the parent component.
             */
            private Component referenceComponent;

            /**
             * {@code 0.0 } = left/top of {@link #referenceComponent}.  {@code 1.0 } for bottom/right or {@link #referenceComponent}.
             */
            private float referencePosition;

            float value;

            byte unit = UNIT_PIXELS;

            int preferredValue;
            int calculatedValue;
            int calculatedBaseValue;
            boolean autoIsClipped;

            /**
             * Calculate the preferred value of this inset.
             * @param parent The parent container.
             * @param cmp The component
             * @return The preferred value of this inset in pixels.
             */
            public int calcPreferredValue(Container parent, Component cmp) {

                if (referenceComponent == null) {
                    // There is no reference component for this inset so we measure
                    // against the parent component directly.
                    switch (unit) {
                        case UNIT_PIXELS:
                            preferredValue = (int) value;
                            break;
                        case UNIT_DIPS:
                            preferredValue = Display.getInstance().convertToPixels(value);
                            break;
                        case UNIT_PERCENT:
                            preferredValue = 0;
                            break;
                        case UNIT_AUTO:
                            preferredValue = 0;
                            break;
                        default:
                            throw new RuntimeException("Invalid unit " + unit);
                    }
                    return preferredValue;
                } else {
                    // There is a reference component so we need to add our own value
                    // to the base inset of the reference component.
                    LayeredLayoutConstraint refCnst = (LayeredLayoutConstraint) getComponentConstraint(referenceComponent);
                    int baseValue = 0;
                    if (refCnst != null) {
                        baseValue = refCnst.insets[side].preferredValue;
                    }
                    
                    // We should have already calculated the preferred size of the 
                    // reference component.
                    //Dimension refPreferredSize = referenceComponent.getPreferredSize();
                    int refPreferredH = getOuterPreferredH(referenceComponent);
                    int refPreferredW = getOuterPreferredW(referenceComponent);
                    
                    if (referencePosition != 0) {
                        // If the inset is not in reference to the edge of the component
                        // then we need to adjust the base value accordingly.
                        switch (side) {
                            case Component.TOP:
                            case Component.BOTTOM:
                                baseValue += ((float) refPreferredH ) * referencePosition;
                                break;
                            default:
                                baseValue += ((float) refPreferredW) * referencePosition;
                        }
                    }
                    
                    // Now we add our own value to the base value.
                    switch (unit) {
                        case UNIT_PIXELS:
                            preferredValue = baseValue + (int) value;
                            break;
                        case UNIT_DIPS:
                            preferredValue = baseValue + Display.getInstance().convertToPixels(value);
                            break;
                        case UNIT_PERCENT:
                            preferredValue = baseValue;
                            break;
                        case UNIT_AUTO:
                            preferredValue = baseValue;
                            break;
                        default:
                            throw new RuntimeException("Invalid unit " + unit);
                    }
                    return preferredValue;
                }
            }

            /**
             * Calculates the "base" value off of which the inset's value should be calculated.
             * It is assumed that the reference component has already been 
             * @param w
             * @param h
             * @return 
             */
            private int calcBaseValue(int top, int left, int bottom, int right) {//, int paddingTop, int paddingLeft, int paddingBottom, int paddingRight) {
                int h = bottom - top;
                int w = right - left;
                int baseValue = 0;
                if (referenceComponent != null) {
                        switch (side) {
                            case Component.TOP:
                                baseValue = getOuterY(referenceComponent) + (int)(getOuterHeight(referenceComponent) * referencePosition) - top;
                                break;
                            case Component.BOTTOM:
                                baseValue = (bottom - getOuterHeight(referenceComponent) - getOuterY(referenceComponent)) + (int)(getOuterHeight(referenceComponent) * referencePosition);
                                break;
                            case Component.LEFT:
                                baseValue = getOuterX(referenceComponent) + (int)(getOuterWidth(referenceComponent) * referencePosition) - left;
                                break;
                            default:
                                baseValue = (right - getOuterWidth(referenceComponent) - getOuterX(referenceComponent)) + (int)(getOuterWidth(referenceComponent)* referencePosition);
                                break;
                        }
                    calculatedBaseValue = baseValue;
                    return baseValue;
                }
                        
                if (referencePosition != 0) {
                    switch (side) {
                        case Component.TOP:
                            baseValue = (int) ((float) h * referencePosition);
                            break;
                        case Component.BOTTOM:
                            baseValue = (int) ((float) h * referencePosition);
                            break;
                        case Component.LEFT:
                            baseValue = (int) ((float) w * referencePosition);
                            break;
                        case Component.RIGHT:
                            baseValue = (int) ((float) w * referencePosition);
                            break;
                        default:
                            throw new RuntimeException("Illegal side for inset: " + side);
                    }
                }
                calculatedBaseValue = baseValue;
                return baseValue;
            }
            
            private boolean isVerticalInset() {
                return side == Component.TOP || side == Component.BOTTOM;
            }
            
            private boolean isHorizontalInset() {
                return side == Component.LEFT || side == Component.RIGHT;
            }
            
            
            
            /**
             * Calculates the actual value of this inset.  This is used inside {@link #layoutComponent(com.codename1.ui.Container, com.codename1.ui.Component, int, int, int, int) }.
             * 
             * @param cmp The component.
             * @param top
             * @param left
             * @param bottom
             * @param right
             * @return The actual value of this inset.
             */
            private int calculate(Component cmp, int top, int left, int bottom, int right) {
                int w = right - left;
                int h = bottom - top;
                int baseValue = calcBaseValue(top, left, bottom ,right);
                autoIsClipped = false;
                switch (unit) {
                    case UNIT_PIXELS:
                        calculatedValue = baseValue + (int) value;
                        break;
                    case UNIT_DIPS:
                        calculatedValue = baseValue + Display.getInstance().convertToPixels(value);
                        break;
                    case UNIT_PERCENT: {
                        Inset oppositeInset = getOppositeInset();
                        
                        int oppositeBaseValue = oppositeInset.calcBaseValue(top, left, bottom, right);
                        if (isVerticalInset()) {
                            calculatedValue = (int)(baseValue + (h - oppositeBaseValue - baseValue) * value / 100f);
                        } else {
                            calculatedValue = (int)(baseValue + (w - oppositeBaseValue - baseValue) * value / 100f);
                        }
                        break;
                    }
                    case UNIT_AUTO: {
                        Inset oppositeInset = getOppositeInset();
                        int oppositeBaseValue = oppositeInset.calcBaseValue(top, left, bottom, right);
                        
                        if (oppositeInset.unit == UNIT_AUTO) {
                            
                            if (isVerticalInset()) {
                                if (cmp.getPreferredH() <= 0) {
                                    calculatedValue = baseValue;
                                    autoIsClipped = true;
                                } else {
                                    calculatedValue = baseValue + (h - oppositeBaseValue - baseValue - getOuterPreferredH(cmp))/2;
                                }
                            } else {
                                if (cmp.getPreferredW() <= 0) {
                                    calculatedValue = baseValue;
                                    autoIsClipped = true;
                                } else {
                                    calculatedValue = baseValue + (w - oppositeBaseValue - baseValue - getOuterPreferredW(cmp))/2;
                                }
                            }
                            if (calculatedValue < 0) {
                                autoIsClipped = true;
                            }
                            calculatedValue = Math.max(0, calculatedValue);
                        } else {
                            if (isVerticalInset()) {
                                if (cmp.getPreferredH() <= 0) {
                                    calculatedValue = baseValue;
                                    autoIsClipped = true;
                                } else {
                                    calculatedValue = h - oppositeInset.calculate(cmp, top, left, bottom, right) - getOuterPreferredH(cmp);
                                }
                                
                            } else {
                                if (cmp.getPreferredW() <= 0) {
                                    calculatedValue = baseValue;
                                    autoIsClipped = true;
                                } else {
                                    calculatedValue = w - oppositeInset.calculate(cmp, top, left, bottom, right) - getOuterPreferredW(cmp);
                                }
                            }
                            if (calculatedValue < 0) {
                                autoIsClipped = true;
                            }
                            calculatedValue = Math.max(0, calculatedValue);
                        }
                        break;
                    }
                    default:
                        throw new RuntimeException("Invalid unit " + unit);
                }

                return calculatedValue;
            }

            public Set<Component> getDependencies(Set<Component> deps) {
                if (referenceComponent != null) {
                    if (deps.contains(referenceComponent)) {
                        return deps;
                    }
                    deps.add(referenceComponent);
                    getOrCreateConstraint(referenceComponent).getDependencies(deps);
                }
                return deps;
            }
            
            public Set<Component> getDependencies() {
                return getDependencies(new HashSet<Component>());
            }

            private Inset getOppositeInset() {
                LayeredLayoutConstraint cnst = LayeredLayoutConstraint.this;
                if (cnst != null) {
                    int oppSide = 0;
                    switch (side) {
                        case Component.TOP:
                            oppSide = Component.BOTTOM;
                            break;
                        case Component.BOTTOM:
                            oppSide = Component.TOP;
                            break;
                        case Component.LEFT:
                            oppSide = Component.RIGHT;
                            break;
                        default:
                            oppSide = Component.LEFT;

                    }
                    return cnst.insets[oppSide];
                }
                return null;
            }
            
            private void setValue(String val) {
                int pos;
                if ((pos=val.indexOf("mm")) != -1) {
                    this.setDips(Float.parseFloat(val.substring(0, pos)));
                } else if ((pos=val.indexOf("px")) != -1) {
                    this.setPixels(Integer.parseInt(val.substring(0, pos)));
                } else if ((pos=val.indexOf("%")) != -1) {
                    this.setPercent(Float.parseFloat(val.substring(0, pos)));
                } else if ("auto".equals(val)) {
                    this.setAuto();
                } else {
                    this.setPixels(Integer.parseInt(val));
                    
                }
            }
            
            public Inset copyTo(Inset dest) {
                dest.autoIsClipped = autoIsClipped;
                dest.calculatedValue = calculatedValue;
                dest.calculatedBaseValue = calculatedBaseValue;
                dest.preferredValue = preferredValue;
                
                dest.value = value;
                dest.unit = unit;
                dest.side = side;
                dest.referenceComponent = referenceComponent;
                dest.referencePosition = referencePosition;
                return dest;
            }
            
            public Inset copyTo(LayeredLayoutConstraint dest) {
                copyTo(dest.insets[side]);
                return this;
            }
            
            public Inset copyTo(Component cmp) {
                copyTo(getOrCreateConstraint(cmp));
                return this;
            }
            
            public Inset copy() {
                return copyTo(new Inset(side));
            }
            
            
            public byte getUnit() {
                return unit;
            }
            
            public boolean isFixed() {
                return unit != UNIT_AUTO;
            }
            
            public float getCurrentValueMM() {
                if (unit == UNIT_DIPS) {
                    return value;
                } else if (unit == UNIT_PIXELS) {
                    float pixelsPerDip = Display.getInstance().convertToPixels(1000)/1000f;
                    return value / pixelsPerDip;
                } else {
                    // In both auto and percent cases, we'll use the existing calculated value as our base
                    float pixelsPerDip = Display.getInstance().convertToPixels(1000)/1000f;
                    int calc = calculatedValue;
                    System.out.println("Calculated value of side "+side+" = "+calc);
                    if (referenceComponent != null) {
                        calc -= calculatedBaseValue;
                    }
                    float out = calc / pixelsPerDip;
                    System.out.println("calc="+out+"mm");
                    return out;
                    
                }
            }
            
            public int getCurrentValuePx() {
                if (unit == UNIT_DIPS) {
                    return Display.getInstance().convertToPixels(value);
                } else if (unit == UNIT_PIXELS) {
                    return (int)value;
                } else {
                    // In both auto and percent cases, we'll use the existing calculated value as our source.
                    int calc = calculatedValue;
                    if (referenceComponent != null) {
                        
                        calc -= calculatedBaseValue;
                    }
                    return calc;
                }
            }
            
            public boolean isVertical() {
                return side == Component.TOP || side == Component.BOTTOM;
            }
            
            public boolean isHorizontal() {
                return side == Component.LEFT || side == Component.RIGHT;
            }
            
            public Inset changeUnits(byte unit) {
                if (unit != this.unit) {
                    if (unit == UNIT_PIXELS) {
                        setPixels(getCurrentValuePx());
                    } else if (unit == UNIT_DIPS) {
                        setDips(getCurrentValueMM());
                    } else if (unit == UNIT_PERCENT) {
                        setDips(getCurrentValueMM());
                    } else {
                        unit(unit);
                    }
                }
                return this;
            }
            
            public Inset changeReference(Container parent, Component newRef, float pos) {
                if (isFlexible()) {
                    // we are flexible, so we'll just set the new reference
                    // and be done
                    referenceComponent(newRef).referencePosition(pos);
                } else {
                    if (newRef != referenceComponent || pos != referencePosition) {
                        LayeredLayoutConstraint cpy = constraint().copy();
                        cpy.insets[side].referenceComponent(newRef).referencePosition(pos);
                        
                        //Container parent = context.getParent();
                        
                        
                        Style s = parent.getStyle();
                        int top = s.getPaddingTop();
                        int bottom = parent.getLayoutHeight() - parent.getBottomGap() - s.getPaddingBottom();
                        int left = s.getPaddingLeft(parent.isRTL());
                        int right = parent.getLayoutWidth() - parent.getSideGap() - s.getPaddingRight(parent.isRTL());
                        int newBase = cpy.insets[side].calcBaseValue(top, left, bottom, right);
                        int oldBase = calcBaseValue(top, left, bottom, right);
                        
                        translatePixels(oldBase - newBase, true, parent);
                        referenceComponent(newRef).referencePosition(pos);
                    }
                }
                
                return this;
                
                
            }
            
            public boolean isFlexible() {
                return unit == UNIT_AUTO;
            }
            
            /**
             * Returns the total inset of this inset when applied to the given component.
             * This will calculate and sum all of the insets of reference components to 
             * get the total inset in pixels from the parent component.
             * @param cmp The component context.
             * @return The total inset in pixels from the parent.
             */
            public int getAbsolutePixels(Component cmp) {
                Container parent = cmp.getParent();
                Style s = parent.getStyle();
                int top = s.getPaddingTop();
                int bottom = parent.getLayoutHeight() - parent.getBottomGap() - s.getPaddingBottom();
                int left = s.getPaddingLeft(parent.isRTL());
                int right = parent.getLayoutWidth() - parent.getSideGap() - s.getPaddingRight(parent.isRTL());
                int baseValue = calcBaseValue(top, left, bottom, right);
                //Rectangle baseRect = getReferenceBox(cmp.getParent(), cmp);
                
                switch (unit) {
                    case UNIT_PIXELS :
                        return baseValue + (int)value;
                    case UNIT_DIPS :
                        return baseValue + Display.getInstance().convertToPixels(value);
                    case UNIT_PERCENT : {
                        
                        Rectangle baseRect = getReferenceBox(parent, cmp);
                        //System.out.println("Baserect is "+baseRect+" baseValue="+baseValue+" for percent "+value);
                        int out = (int)(baseValue + (isHorizontalInset() ? baseRect.getWidth() : baseRect.getHeight()) * value / 100f);
                        //System.out.println("Result is "+out);
                        return out;
                    }
                    case UNIT_AUTO : {
                        Inset oppositeInset = getOppositeInset();
                        if (oppositeInset.unit == UNIT_AUTO) {
                            Rectangle baseRect = getReferenceBox(parent, cmp);
                            // they're both auto, 
                            //int oppositeBase = oppositeInset.calcBaseValue(top, left, bottom, right);
                            if (isVerticalInset()) {
                                return (baseRect.getHeight() - getOuterPreferredH(cmp)) / 2;
                            } else {
                                return (baseRect.getWidth() - getOuterPreferredW(cmp)) / 2;
                            }
                        } else {
                            if (isVerticalInset()) {
                                return bottom - top - oppositeInset.getAbsolutePixels(cmp) - baseValue - getOuterPreferredH(cmp);
                            } else {
                                //System.out.println("Checking opposite inset for value");
                                int out =  right - left - oppositeInset.getAbsolutePixels(cmp) - baseValue - getOuterPreferredW(cmp);
                                //System.out.println("Auto value is "+out);
                                return out;
                            }
                        }
                    }
                    default :
                        throw new RuntimeException("Illegal state in inset.  Unknown unit "+unit);
                        
                }
                
            }
            
            public Inset translatePixels(int delta, boolean preferMM, Container parent) {
                
                switch (unit) {
                    case UNIT_PIXELS :
                        value += delta;
                        break;
                    case UNIT_DIPS : {
                        float pixelsPerDip = Display.getInstance().convertToPixels(1000)/1000f;
                        //System.out.println("Old dips for side "+side+" = "+value);
                        value += (delta / pixelsPerDip);
                        //System.out.println("New dips for side "+side+" = "+value);
                        break;
                    }
                    case UNIT_PERCENT: {
                        //Container parent = cmp.getParent();
                        //Style parentStyle = parent.getStyle();
                        Style s = parent.getStyle();
                        int top = s.getPaddingTop();
                        int bottom = parent.getLayoutHeight() - parent.getBottomGap() - s.getPaddingBottom();
                        int left = s.getPaddingLeft(parent.isRTL());
                        int right = parent.getLayoutWidth() - parent.getSideGap() - s.getPaddingRight(parent.isRTL());
                        int baseValue = calculatedBaseValue;
                        int oppositeBaseValue = getOppositeInset().calculatedBaseValue;
                        if (isVerticalInset()) {
                            float relH = bottom - top - baseValue - oppositeBaseValue;
                            if (Math.abs(relH) < 1f) {
                                return this;
                            }
                            float percentDelta = delta / relH * 100f;
                            value += percentDelta;
                            
                        } else {
                            float relH = right - left - baseValue - oppositeBaseValue;
                            //System.out.println("relH="+relH+" delta="+delta);
                            if (Math.abs(relH) < 1f) {
                                return this;
                            }
                            float percentDelta = delta / relH * 100f;
                            //System.out.println("percentDelta="+percentDelta);
                            value += percentDelta;
                            //System.out.println("Value="+value);
                        }
                        break;
                        
                    }
                    case UNIT_AUTO : {
                        // If this is auto then we'll need to make it fixed... but we'll start
                        // by making it fixed
                        unit = preferMM ? UNIT_DIPS : UNIT_PIXELS;
                        if (unit == UNIT_PIXELS) {
                            value = calculatedValue + delta - calculatedBaseValue;
                        } else {
                            float pixelsPerDip = Display.getInstance().convertToPixels(1000)/1000f;
                            value = (calculatedValue + delta - calculatedBaseValue) / pixelsPerDip;
                        }
                        break;
                    }
                        
                }
                calculatedValue += delta;
                return this;
            }
            
            public Inset translateMM(float delta, boolean preferMM, Container parent) {
                return translatePixels(Display.getInstance().convertToPixels(delta), preferMM, parent);
                /*
                switch (unit) {
                    case UNIT_DIPS :
                        value += delta;
                        break;
                    case UNIT_PIXELS : {
                        value += Display.getInstance().convertToPixels(delta);
                        break;
                    }
                    case UNIT_PERCENT: {
                        return translatePixels(Display.getInstance().convertToPixels(delta), preferMM, cmp);
                    }
                    case UNIT_AUTO : {
                        // If this is auto then we'll need to make it fixed... but we'll start
                        // by making it fixed
                        unit = preferMM ? UNIT_DIPS : UNIT_PIXELS;
                        if (unit == UNIT_PIXELS) {
                            value = calculatedValue + Display.getInstance().convertToPixels(delta);
                        } else {
                            float pixelsPerDip = Display.getInstance().convertToPixels(1000)/1000f;
                            value = calculatedValue / pixelsPerDip + delta;
                        }
                        break;
                    }
                        
                }
                return this;
                        */
            }

        }

    }

}