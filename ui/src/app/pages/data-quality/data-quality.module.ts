import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgbNavModule, NgbTooltipModule, NgbDropdownModule, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { NgSelectModule } from '@ng-select/ng-select';
import { LoaderModule } from 'src/app/core/components/loader/loader.module';
import { DataQualityRoutingModule } from './data-quality-routing.module';
import { DataQualityComponent } from './data-quality.component';
import { DataQualityListComponent } from './components/data-quality-list/data-quality-list.component';
import { DataQualityCreateComponent } from './components/data-quality-create/data-quality-create.component';
import { DataQualityShowDataComponent } from './components/data-quality-show-data/data-quality-show-data.component';
import { DataQualityEditComponent } from './components/data-quality-edit/data-quality-edit.component';
import { ConfirmationComponent } from './components/confirmation/confirmation.component';
import { RuleTypesComponent } from './components/rule-types/rule-types.component';
import { EditRuleComponent } from './components/edit-rule/edit-rule.component';

@NgModule({
    declarations: [
        DataQualityComponent,
        DataQualityListComponent,
        DataQualityCreateComponent,
        DataQualityShowDataComponent,
        DataQualityEditComponent,
        ConfirmationComponent,
        RuleTypesComponent,
        EditRuleComponent
    ],
    imports: [
        CommonModule,
        DataQualityRoutingModule,
        ReactiveFormsModule,
        NgbNavModule,
        NgbTooltipModule,
        LoaderModule,
        FormsModule,
        NgSelectModule,
        NgbDropdownModule,
        NgbModule
    ],
    // schemas: [NO_ERRORS_SCHEMA]
})
export class DataQualityModule { }
