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
package org.eclipse.sirius.web.papaya.representations.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IEditingContextRepresentationDescriptionProvider;
import org.eclipse.sirius.components.core.api.IIdentityService;
import org.eclipse.sirius.components.core.api.ILabelService;
import org.eclipse.sirius.components.papaya.Package;
import org.eclipse.sirius.components.papaya.PapayaFactory;
import org.eclipse.sirius.components.papaya.PapayaPackage;
import org.eclipse.sirius.components.papaya.spec.PackageSpec;
import org.eclipse.sirius.components.representations.IRepresentationDescription;
import org.eclipse.sirius.components.representations.VariableManager;
import org.eclipse.sirius.components.tables.descriptions.CheckboxCellDescription;
import org.eclipse.sirius.components.tables.descriptions.ColumnDescription;
import org.eclipse.sirius.components.tables.descriptions.ICellDescription;
import org.eclipse.sirius.components.tables.descriptions.IconLabelCellDescription;
import org.eclipse.sirius.components.tables.descriptions.LineDescription;
import org.eclipse.sirius.components.tables.descriptions.MultiSelectCellDescription;
import org.eclipse.sirius.components.tables.descriptions.SelectCellDescription;
import org.eclipse.sirius.components.tables.descriptions.TableDescription;
import org.eclipse.sirius.components.tables.descriptions.TextfieldCellDescription;
import org.springframework.stereotype.Service;

/**
 * This class is used to provide the description of a table on Papaya package element. <br> This is an example to demonstrate how to use a table description.
 *
 * @author Jerome Gout
 */
@Service
public class PackageTableRepresentationDescriptionProvider implements IEditingContextRepresentationDescriptionProvider {

    public static final String TABLE_DESCRIPTION_ID = "papaya_package_table_description";

    private final IIdentityService identityService;

    private final ILabelService labelService;

    private final ComposedAdapterFactory composedAdapterFactory;

    public PackageTableRepresentationDescriptionProvider(IIdentityService identityService, ILabelService labelService, ComposedAdapterFactory composedAdapterFactory) {
        this.identityService = Objects.requireNonNull(identityService);
        this.labelService = Objects.requireNonNull(labelService);
        this.composedAdapterFactory = Objects.requireNonNull(composedAdapterFactory);
    }

    @Override
    public List<IRepresentationDescription> getRepresentationDescriptions(IEditingContext editingContext) {
        Function<VariableManager, String> headerLabelProvider = variableManager -> variableManager.get(VariableManager.SELF, Object.class)
                .map(this.labelService::getLabel)
                .orElse(null);

        Function<VariableManager, List<String>> headerIconURLsProvider = variableManager -> variableManager.get(VariableManager.SELF, Object.class)
                .map(this.labelService::getImagePath)
                .orElse(List.of());

        Function<VariableManager, String> headerIndexLabelProvider = variableManager -> variableManager.get("rowIndex", Integer.class)
                .map(String::valueOf)
                .orElse(null);

        var lineDescription = LineDescription.newLineDescription(UUID.nameUUIDFromBytes("Table - Line".getBytes()))
                .targetObjectIdProvider(new TableTargetObjectIdProvider(this.identityService))
                .targetObjectKindProvider(new TableTargetObjectKindProvider(this.identityService))
                .semanticElementsProvider(this::getSemanticElements)
                .headerLabelProvider(headerLabelProvider)
                .headerIconURLsProvider(headerIconURLsProvider)
                .headerIndexLabelProvider(headerIndexLabelProvider)
                .build();

        var tableDescription = TableDescription.newTableDescription(TABLE_DESCRIPTION_ID)
                .label("Papaya package table")
                .labelProvider(new TableLabelProvider(this.labelService))
                .canCreatePredicate(this::canCreate)
                .lineDescriptions(List.of(lineDescription))
                .columnDescriptions(this.getColumnDescriptions())
                .targetObjectIdProvider(new TableTargetObjectIdProvider(this.identityService))
                .targetObjectKindProvider(new TableTargetObjectKindProvider(this.identityService))
                .cellDescriptions(this.getCellDescriptions())
                .iconURLsProvider(variableManager -> List.of("/papaya-representations/package-table.svg"))
                .isStripeRowPredicate(variableManager -> true)
                .build();

        return List.of(tableDescription);
    }


    private boolean canCreate(VariableManager variableManager) {
        return variableManager.get(VariableManager.SELF, Object.class)
                .filter(PackageSpec.class::isInstance)
                .isPresent();
    }

    private List<Object> getAllTypesOfPackage(Package aPackage) {
        var result = aPackage.getTypes().stream()
                .map(Object.class::cast)
                .collect(Collectors.toList());
        result.addAll(aPackage.getPackages().stream()
                .flatMap(pack -> this.getAllTypesOfPackage(pack).stream())
                .toList());
        return result;
    }

    private List<Object> getSemanticElements(VariableManager variableManager) {
        return variableManager.get(VariableManager.SELF, PackageSpec.class)
                .map(this::getAllTypesOfPackage)
                .orElse(List.of());
    }

    private List<ColumnDescription> getColumnDescriptions() {
        var provider = new StructuralFeatureToDisplayNameProvider(new DisplayNameProvider(this.composedAdapterFactory));
        Map<EStructuralFeature, String> featureToDisplayName = provider.getColumnsStructuralFeaturesDisplayName(PapayaFactory.eINSTANCE.createClass(), PapayaPackage.eINSTANCE.getType());

        ColumnDescription iconColumnDescription = ColumnDescription.newColumnDescription(UUID.nameUUIDFromBytes("icon".getBytes()))
                .semanticElementsProvider(variableManager -> List.of("IconColumn"))
                .headerLabelProvider(variableManager -> "Icon")
                .headerIconURLsProvider(variableManager -> List.of("/icons/svg/Default.svg"))
                .headerIndexLabelProvider(variableManager -> "")
                .targetObjectIdProvider(new ColumnTargetObjectIdProvider())
                .targetObjectKindProvider(variableManager -> "")
                .initialWidthProvider(variableManager -> 130)
                .isResizablePredicate(variableManager -> false)
                .build();

        Function<VariableManager, String> headerLabelProvider = variableManager -> variableManager.get(VariableManager.SELF, EStructuralFeature.class)
                .map(featureToDisplayName::get)
                .orElse("");

        Function<VariableManager, List<String>> headerIconURLsProvider = variableManager -> variableManager.get(VariableManager.SELF, EStructuralFeature.class)
                .map(this.labelService::getImagePath)
                .orElse(List.of());

        Function<VariableManager, String> headerIndexLabelProvider = variableManager -> variableManager.get("columnIndex", Integer.class)
                .map(index -> String.valueOf((char) (index + 'A')))
                .orElse("");

        ColumnDescription columnDescription = ColumnDescription.newColumnDescription(UUID.nameUUIDFromBytes("features".getBytes()))
                .semanticElementsProvider(variableManager -> featureToDisplayName.keySet().stream().map(Object.class::cast).toList())
                .headerLabelProvider(headerLabelProvider)
                .headerIconURLsProvider(headerIconURLsProvider)
                .headerIndexLabelProvider(headerIndexLabelProvider)
                .targetObjectIdProvider(new ColumnTargetObjectIdProvider())
                .targetObjectKindProvider(variableManager -> "")
                .initialWidthProvider(variableManager -> 180)
                .isResizablePredicate(variableManager -> true)
                .build();
        return List.of(iconColumnDescription, columnDescription);
    }

    private List<ICellDescription> getCellDescriptions() {
        List<ICellDescription> cellDescriptions = new ArrayList<>();

        cellDescriptions.add(TextfieldCellDescription.newTextfieldCellDescription("textfieldCells")
                .canCreatePredicate(new CellTypePredicate().isTextfieldCell())
                .targetObjectIdProvider(new TableTargetObjectIdProvider(this.identityService))
                .targetObjectKindProvider(new TableTargetObjectKindProvider(this.identityService))
                .cellValueProvider(new CellStringValueProvider(this.identityService))
                .build());

        cellDescriptions.add(CheckboxCellDescription.newCheckboxCellDescription("checkboxCells")
                .canCreatePredicate(new CellTypePredicate().isCheckboxCell())
                .targetObjectIdProvider(new TableTargetObjectIdProvider(this.identityService))
                .targetObjectKindProvider(new TableTargetObjectKindProvider(this.identityService))
                .cellValueProvider(new CellBooleanValueProvider())
                .build());

        cellDescriptions.add(SelectCellDescription.newSelectCellDescription("selectCells")
                .canCreatePredicate(new CellTypePredicate().isSelectCell())
                .targetObjectIdProvider(new TableTargetObjectIdProvider(this.identityService))
                .targetObjectKindProvider(new TableTargetObjectKindProvider(this.identityService))
                .cellValueProvider(new CellStringValueProvider(this.identityService))
                .cellOptionsIdProvider(new CellOptionIdProvider(this.identityService, this.labelService))
                .cellOptionsLabelProvider(new CellOptionLabelProvider(this.labelService))
                .cellOptionsProvider(new CellOptionsProvider(this.composedAdapterFactory))
                .build());

        cellDescriptions.add(MultiSelectCellDescription.newMultiSelectCellDescription("multiselectCells")
                .canCreatePredicate(new CellTypePredicate().isMultiselectCell())
                .targetObjectIdProvider(new TableTargetObjectIdProvider(this.identityService))
                .targetObjectKindProvider(new TableTargetObjectKindProvider(this.identityService))
                .cellValueProvider(new CellStringListValueProvider(this.identityService))
                .cellOptionsIdProvider(new CellOptionIdProvider(this.identityService, this.labelService))
                .cellOptionsLabelProvider(new CellOptionLabelProvider(this.labelService))
                .cellOptionsProvider(new CellOptionsProvider(this.composedAdapterFactory))
                .build());


        Predicate<VariableManager> canCreateIconLabelPredicate = variableManager -> variableManager.get(ColumnDescription.COLUMN_TARGET_OBJECT, Object.class)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(value -> value.equals("IconColumn"))
                .isPresent();

        BiFunction<VariableManager, Object, List<String>> iconLabelCellIconURLsProvider = (variableManager, columnTargetObject) -> variableManager.get(VariableManager.SELF, EObject.class)
                .map(this.labelService::getImagePath)
                .orElse(List.of());

        cellDescriptions.add(IconLabelCellDescription.newIconLabelCellDescription("iconLabelCells")
                .canCreatePredicate(canCreateIconLabelPredicate)
                .targetObjectIdProvider(new TableTargetObjectIdProvider(this.identityService))
                .targetObjectKindProvider(new TableTargetObjectKindProvider(this.identityService))
                .cellValueProvider((variableManager, columnTargetObject) -> "")
                .cellIconURLsProvider(iconLabelCellIconURLsProvider)
                .build());
        return cellDescriptions;
    }
}