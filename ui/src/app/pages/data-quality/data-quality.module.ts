import { NgModule, NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgbNavModule, NgbTooltipModule, NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { NgSelectModule } from '@ng-select/ng-select';
import { LoaderModule } from 'src/app/core/components/loader/loader.module';
import { DataQualityRoutingModule } from './data-quality-routing.module';
import { DataQualityComponent } from './data-quality.component';
import { DataQualityListComponent } from './components/data-quality-list/data-quality-list.component';
import { DataQualityCreateComponent } from './components/data-quality-create/data-quality-create.component';
import { DataQualityShowDataComponent } from './components/data-quality-show-data/data-quality-show-data.component';

@NgModule({
    declarations: [
        DataQualityComponent,
        DataQualityListComponent,
        DataQualityCreateComponent,
        DataQualityShowDataComponent,
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
        NgbDropdownModule
    ],
    // schemas: [NO_ERRORS_SCHEMA]
})
export class DataQualityModule { }
