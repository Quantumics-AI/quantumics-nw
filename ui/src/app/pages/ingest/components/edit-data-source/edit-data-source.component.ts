import { Component, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { SourceDataService } from '../../services/source-data.service';

@Component({
  selector: 'app-edit-data-source',
  templateUrl: './edit-data-source.component.html',
  styleUrls: ['./edit-data-source.component.scss']
})
export class EditDataSourceComponent implements OnInit {
  @Input() projectId: number;
  @Input() userId: number;
  @Input() sourceData: any;
  @Input() awsId: number;
  // @Input() rolename: string;

  fg: FormGroup;
  public accessType: string;
  // public policyData: any = [
  //   {
  //     id: 1,
  //     policyName: "IAM",
  //   },
  //   {
  //     id: 2,
  //     policyName: "Resource Policy",
  //   },
  //   {
  //     id: 3,
  //     policyName: "Secret Key / Access Key"
  //   }
  // ];

  public accessData:any;
  transformedData: any[] = [];
  public selectedType: string;

  constructor(
    public modal: NgbActiveModal,
    private snakbar: SnackbarService,
    private fb: FormBuilder,
    private sourceDataService: SourceDataService,
  ){

  }

  ngOnInit(): void {
    this.getAccessTypes();
    this.fg = this.fb.group({
      sourceId: [this.sourceData.id],
      sourceName : [this.sourceData.connectionName, Validators.required],
      connectionType:[{ value: this.sourceData.accessType, disabled: true }],
      // roleName:[{ value: this.rolename, disabled: true }],
      subDataSource: [{ value: this.sourceData.subDataSource, disabled: true }, Validators.required],
      bucketName:[{ value: this.sourceData.bucketName, disabled: true }],
    })
  }

  public getAccessTypes(): void {
    this.sourceDataService.getAccessTypes().subscribe((res) => {
      console.log(res);
      this.accessData = res;
      this.transformData();
      
    }, (error) => {

    });
  }

  public transformData() {
    let id = 1;
    for (const key in this.accessData) {
      if (this.accessData.hasOwnProperty(key)) {
        const type = this.accessData[key];
        this.transformedData.push({ id, type });
        id++;
      }
    }
    
  }


  public updateData(): void {
    
    const request = {
      projectId: this.projectId,
      userId: this.userId,
      connectionName: this.fg.value.sourceName,
      accessType: this.sourceData.accessType,
      // connectionData: this.sourceData.connectionData,
      subDataSource: this.fg.controls.subDataSource.value,
      bucketName: this.fg.controls.bucketName.value
    }

    this.sourceDataService.updateSourceData(this.awsId, request).subscribe((response) => {
      // if (response.code === 200) {
        this.modal.close(response);
        this.snakbar.open("Data source updated successfully!");
      // }
    }, (error) => {
      this.snakbar.open(error);
      this.modal.close();
    });
    // this.snakbar.open("Data source updated successfully!")
    // this.modal.close();
  }
}
