import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';
import { Location } from '@angular/common';
import { AwsData, RoleData } from '../../models/awsdata';
import { SourceDataService } from '../../services/source-data.service';

@Component({
  selector: 'app-create-aws',
  templateUrl: './create-aws.component.html',
  styleUrls: ['./create-aws.component.scss']
})
export class CreateAwsComponent {
  private certificate$: Observable<Certificate>;
  private certificateData: Certificate;
  private unsubscribe: Subject<void> = new Subject();
  public projectId: number;
  public awsData: AwsData;
  public userId: number;

  public fg: FormGroup;
  public loading: boolean;
  public queryFolders: boolean = true;
  public selectedPolicyName: string;
  public policyData: any = [
    {
      id: 1,
      policyName: "IAM",
    },
    {
      id: 2,
      policyName: "Resource Policy",
    },
    {
      id: 3,
      policyName: "Secret Key / Access Key"
    }
  ];
  public connection: boolean = false;
  invalidPattern: boolean = false;
  invalidCapitalPattern: boolean = false;
  public connectionParams: AwsData;
  public accessData: any;
  transformedData: any[] = [];
  public sourceListData: any;
  public alreadyExist: boolean = false;

  constructor(
    private fb: FormBuilder,
    private snakbar: SnackbarService,
    private activatedRoute: ActivatedRoute,
    private quantumFacade: Quantumfacade,
    private router: Router,
    private location: Location,
    private sourceDataService: SourceDataService,
  ){
    // this.awsData = new AwsData();
    this.certificate$ = this.quantumFacade.certificate$;
    this.certificate$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(certificate => {
        if (certificate) {
          this.certificateData = certificate;
          this.userId = +this.certificateData.user_id;
        }
      });
  }

  ngOnInit(): void {
    this.projectId = +this.activatedRoute.parent.snapshot.paramMap.get('projectId');
      //End space allowed    /^([A-Z]).*([A-Za-z0-9_:-]+\s*)*[A-Za-z0-9_:-]+\s*$/
    this.fg = this.fb.group({
      dataSourceName: ['', [Validators.required, Validators.pattern(/^([A-Z]).([A-Za-z0-9_:-]+\s)*[A-Za-z0-9_:-]+$/)]],
      connectionType: [{ value: 'PROFILE', disabled: true }],
      subDataSource: [{ value: 'S3', disabled: true }, Validators.required],
      // roleName: ['Shivraj', Validators.required]
    });

    this.getAccessTypes();
    this.getAwsList()
  }

  public getAwsList(): void {
    this.loading = true;
    this.sourceDataService.getSourceData(+this.projectId, this.userId).subscribe((response) => {
      this.loading = false;
      this.sourceListData = response;
      if (this.sourceListData.length > 1) {
        this.sourceListData.sort((val1, val2) => {
          return (
            (new Date(val2.createdDate) as any) -
            (new Date(val1.createdDate) as any)
          );
        });
      }
    }, (error) => {
      this.loading = false;
    })
  }

  checkIfNameExists() {
    if (this.sourceListData.length > 0) {
      const value = this.fg.get('dataSourceName').value;
      const titleCaseValue = value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();

      // const enteredName = this.fg.get('dataSourceName').value;
      const nameExists = this.sourceListData.some(item => item.connectionName === titleCaseValue);
      
      if (nameExists) {
        this.alreadyExist = true;
      } else {
        this.alreadyExist = false;
      }
    }
    
  }


  public getAccessTypes(): void {
    this.sourceDataService.getAccessTypes().subscribe((res) => {
      console.log(res);
      this.accessData = res;
      console.log(this.accessData);
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

    console.log(this.transformedData);
    
  }


  public onSelectAcceptType(type: any): void {
    // const obj = this.transformedData.find(x => x.type === 'PROFILE');
    // if (obj) {
    //   this.fg.controls.connectionType.setValue(obj.type);
    // }
  }

  modelChangeDataSourceName(str) {
    // const re = /^[a-zA-Z0-9_]+$/;

    if (this.fg.controls.dataSourceName.value != "") {

      const validCapital = String(str).match(/^([A-Z])/);
      
      if (validCapital == null) {
        this.invalidCapitalPattern = true;
      } else {
        this.invalidCapitalPattern = false;
        const validWorkSpaceName = String(str)
        .match(
          /^([A-Z]).([A-Za-z0-9_:-]+\s)*[A-Za-z0-9_:-]+$/
        );

        if (validWorkSpaceName == null) {
          this.invalidPattern = true;

        } else {
          this.invalidPattern = false;
        }
      }
    } else {
      this.invalidPattern = false;
      this.invalidCapitalPattern = false;
    }

  }



  public continue(): void {
    this.router.navigate([`projects/${this.projectId}/ingest/select-source-target`]);
  }

  public testConnection(): void {

    // const role_data = {
    //   iam: this.fg.controls.roleName.value,
    // } as RoleData;

    const value = this.fg.get('dataSourceName').value;
    const titleCaseValue = value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
    this.fg.get('dataSourceName').setValue(titleCaseValue);

    this.connectionParams = {
      projectId: this.projectId,
      userId: this.userId,
      connectionName: this.fg.controls.dataSourceName.value,
      accessType: this.fg.controls.connectionType.value,
      subDataSource: this.fg.controls.subDataSource.value,
      bucketName: '',
    } as AwsData;
    console.log(this.connectionParams);

    this.sourceDataService.testConnection(this.userId,this.projectId, this.connectionParams).subscribe((response) => {
      console.log("--", response);
      
      sessionStorage.setItem('awsData', JSON.stringify(this.connectionParams));
      this.snakbar.open(response.message);
      this.connection = true;
    }, (error) => {
      this.snakbar.open(error);
    });
    
  }

  public back(): void {
    this.location.back();
  }
}
