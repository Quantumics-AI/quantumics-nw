import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DataQualityComponent } from './data-quality.component';
import { DataQualityListComponent } from './components/data-quality-list/data-quality-list.component';
import { DataQualityCreateComponent } from './components/data-quality-create/data-quality-create.component';
import { DataQualityEditComponent } from './components/data-quality-edit/data-quality-edit.component';
import { RuleTypesComponent } from './components/rule-types/rule-types.component';

const routes: Routes = [
    {
        path: '',
        component: DataQualityListComponent
    },
    {
        path: 'create',
        component: DataQualityCreateComponent
    },
    {
        path: 'edit',
        component: DataQualityEditComponent
    },
    {
        path: 'rule-types',
        component: RuleTypesComponent
    },
    //   {
    //     path: '',
    //     component: FileProfileComponent,
    //     children: [
    //       {
    //         path: '',
    //         pathMatch: 'full',
    //         redirectTo: 'data-profile'
    //       },
    //       {
    //         path: 'data-profile',
    //         component: ViewFileProfileComponent,
    //       }
    //     ]
    //   }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DataQualityRoutingModule { }
