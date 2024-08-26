/*******************************************************************************
 * Copyright (c) 2024 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.sirius.components.papaya.provider.spec;

import java.util.List;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.edit.provider.ComposedImage;
import org.eclipse.emf.edit.provider.IItemStyledLabelProvider;
import org.eclipse.emf.edit.provider.StyledString;
import org.eclipse.sirius.components.papaya.Constructor;
import org.eclipse.sirius.components.papaya.NamedElement;
import org.eclipse.sirius.components.papaya.provider.ConstructorItemProvider;
import org.eclipse.sirius.components.papaya.provider.PapayaItemProviderAdapterFactory;
import org.eclipse.sirius.components.papaya.provider.spec.images.VisibilityOverlayImageProvider;

/**
 * Customization of the item provider implementation generated by EMF.
 *
 * @author sbegaudeau
 */
public class ConstructorItemProviderSpec extends ConstructorItemProvider {
    public ConstructorItemProviderSpec(AdapterFactory adapterFactory) {
        super(adapterFactory);
    }

    @Override
    public Object getImage(Object object) {
        if (object instanceof Constructor constructor) {
            var visibilityImage = new VisibilityOverlayImageProvider().overlayImage(this.getResourceLocator(), constructor.getVisibility());

            return new ComposedImage(List.of(
                    this.getResourceLocator().getImage("full/obj16/Constructor.svg"),
                    visibilityImage
            ));
        }
        return this.overlayImage(object, this.getResourceLocator().getImage("full/obj16/Constructor.svg"));
    }

    @Override
    public Object getStyledText(Object object) {
        if (object instanceof Constructor constructor) {
            if (constructor.eContainer() instanceof NamedElement namedElement && namedElement.getName() != null && !namedElement.getName().isBlank()) {
                StyledString styledLabel = new StyledString();
                styledLabel.append(namedElement.getName());

                styledLabel.append("(", PapayaStyledStringStyles.DECORATOR_STYLE);

                for (var i = 0; i < constructor.getParameters().size(); i++) {
                    var parameter = constructor.getParameters().get(i);

                    var adapter = new PapayaItemProviderAdapterFactory().adapt(parameter, IItemStyledLabelProvider.class);
                    if (adapter instanceof IItemStyledLabelProvider itemStyledLabelProvider) {
                        var rawStyledString = itemStyledLabelProvider.getStyledText(parameter);
                        if (rawStyledString instanceof StyledString styledString) {
                            styledLabel.append(styledString);
                        }
                    }

                    if (i < constructor.getParameters().size() - 1) {
                        styledLabel.append(", ", PapayaStyledStringStyles.DECORATOR_STYLE);
                    }
                }

                styledLabel.append(")", PapayaStyledStringStyles.DECORATOR_STYLE);
                return styledLabel;
            }
        }
        return super.getStyledText(object);
    }
}
