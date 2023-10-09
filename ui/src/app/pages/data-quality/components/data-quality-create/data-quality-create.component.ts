import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';
import { Location } from '@angular/common';
import { BrowseFileComponent } from '../browse-file/browse-file.component';
import { RuleCreationService } from '../../services/rule-creation.service';

@Component({
  selector: 'app-data-quality-create',
  templateUrl: './data-quality-create.component.html',
  styleUrls: ['./data-quality-create.component.scss']
})
export class DataQualityCreateComponent implements OnInit {
  private certificate$: Observable<Certificate>;
  private certificateData: Certificate;
  private unsubscribe: Subject<void> = new Subject();
  public fg: FormGroup;
  public sourceTargetType: boolean = true;
  public isShowData: boolean = false;
  public projectId: number;
  userId: number;
  public selectedNullName: string;
  public ruleTypesData: any;
  public selectedMainRuleType: string;
  public selectedSubLevel: string;
  invalidPattern: boolean = false;
  invalidCapitalPattern: boolean = false;
  invalidDesPattern: boolean = false;
  public dataConnectionList: any;
  public selectedDataConnections1: string;
  public selectedDataConnections2: string;
  public selectedAttributeName: string;
  public selectedMultipleAttributeName: string;
  public bucketList: any;
  public selectedBucketOne: string;
  public selectedBucketTwo: string;
  public connectionParams: any;
  public bucketSourceOne: any = [];
  public bucketSourceTwo: any = [];
  public saveRulePayload: any;
  public selectedSourcePathOne: string;
  public selectedSourcePathTwo: string;
  public formData: any;
  public selectedPath: string;
  public selectedFile: string;
  public selectedSource: string;
  public filecheck: boolean;
  public columnData: any;

  constructor(
    private fb: FormBuilder,
    private snakbar: SnackbarService,
    private activatedRoute: ActivatedRoute,
    private quantumFacade: Quantumfacade,
    private router: Router,
    private location: Location,
    private ruleCreationService: RuleCreationService,
    private cdr: ChangeDetectorRef) {
      this.formData = JSON.parse(sessionStorage.getItem('formData'));
      this.selectedPath = sessionStorage.getItem('SelectedPath');
      this.selectedFile = sessionStorage.getItem('selectedFile');
      this.selectedSource = sessionStorage.getItem('source');
      this.filecheck = JSON.parse(sessionStorage.getItem('check'));
      this.columnData = JSON.parse(sessionStorage?.getItem('headersData'));
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
    const ruleDescriptionPattern = /^[A-Za-z\s!@#$%^&*()\-_+=\[\]{}|;:'",.<>?/]*$/;
    this.projectId = +this.activatedRoute.parent.snapshot.paramMap.get('projectId');
    this.fg = this.fb.group({ 
      // '^[A-Za-z\\s!@#$%^&*()\\-_+=\\[\\]{}|;:\'",.<>?/]*$'
      ruleName: ['', [Validators.required, Validators.pattern(/^([A-Z]).([A-Za-z0-9_:-]+\s)*[A-Za-z0-9_:-]+$/)]],
      ruleDescription: ['', [Validators.required, Validators.pattern(ruleDescriptionPattern)]],
      sourceAndTarget: [true],
      //source
      //source1 - source2
      sourceDataSource: new FormControl('aws'),
      subDataSourceOne: new FormControl('s3'),
      sourceDataConnection: new FormControl(),
      sourceBucketOne: new FormControl(''),
      sourceFolderPath: new FormControl({ value: '', disabled: true }),
      //
      sourceDataSourceTwo: new FormControl('aws'),
      subDataSourceTwo: new FormControl('s3'),
      sourceDataConnectionTwo: new FormControl(),
      sourceBucketTwo: new FormControl(''),
      sourceFolderPathTwo: new FormControl({ value: '', disabled: true }),
      //types
      ruleType: new FormControl('', Validators.required),
      subLavelRadio: new FormControl(''),
      selectColumnAttribute: new FormControl(''),
      selectMultipleAttribute: new FormControl(''),
      selectNullAttributes: new FormControl(''),
      acceptancePercentage: new FormControl(''),
      //
    });

    this.getDataConnection();
    this.getRuleTypeList();
    this.getBucketData();
  }

  ngAfterViewInit(): void {
    if (this.formData) {
      this.fg.controls.ruleName.setValue(this.formData.ruleName);
      this.fg.controls.ruleDescription.setValue(this.formData.ruleDescription);
      this.fg.controls.sourceAndTarget.setValue(this.formData?.sourceAndTarget);
      this.fg.controls.sourceDataConnection.setValue(this.formData?.sourceDataConnection);
      this.fg.controls.sourceBucketOne.setValue(this.formData?.sourceBucketOne);
      this.bucketSourceOne.push(this.formData?.sourceBucketOne);
      if (this.filecheck) {
        if (this.selectedSource == 'source-1') {
          this.fg.controls.sourceFolderPath.setValue(this.selectedPath);
          this.fg.controls.sourceFolderPathTwo.setValue(this.formData?.sourceFolderPathTwo);
  
        } else {
          this.fg.controls.sourceFolderPath.setValue(this.formData?.sourceFolderPath);
          this.fg.controls.sourceFolderPathTwo.setValue(this.selectedPath); 
        }
      } else {
        this.fg.controls.sourceFolderPath.setValue(this.formData?.sourceFolderPath);
        this.fg.controls.sourceFolderPathTwo.setValue(this.formData?.sourceFolderPathTwo);
      }
      this.fg.controls.sourceDataConnectionTwo.setValue(this.formData?.sourceDataConnectionTwo);
      this.fg.controls.sourceBucketTwo.setValue(this.formData?.sourceBucketTwo);
      this.bucketSourceTwo.push(this.formData?.sourceBucketTwo);
      
    } else {
      console.log("not added");
    }
  }

  public getDataConnection(): void {
    this.ruleCreationService.getDataConnection(this.projectId, this.userId).subscribe((response) => {
      this.dataConnectionList = response;
    }, (error) => {

    })
  }

  public getRuleTypeList(): void {
    this.ruleCreationService.getRuleTypes(this.projectId, !this.sourceTargetType).subscribe((response) => {
      this.ruleTypesData = response.result;
    }, (error) => {

    })
  }

  public getBucketData(): void {
    this.ruleCreationService.getBucketList(this.userId,this.projectId).subscribe((response) => {
      this.bucketList = response;
    }, (error) => {

    });
  }


  // change radio button 
  public selectedType(type: boolean): void {
    this.getRuleTypeList();
    // if (type === 'sourceAndTarget') {
    //   this.fg.get('sourceDataSource').setValidators([Validators.required]);
    //   this.fg.get('sourceDataConnection').setValidators([Validators.required]);
    //   this.fg.get('sourceBucket').setValidators([Validators.required]);
    //   this.fg.get('sourceFolder').setValidators([Validators.required]);
    // } else {
    //   this.fg.get('sourceDataSource')?.clearValidators();
    //   this.fg.get('sourceDataConnection')?.clearValidators();
    //   this.fg.get('sourceBucket')?.clearValidators();
    //   this.fg.get('sourceFolder')?.clearValidators();
    // }

    // this.fg.get('sourceDataSource')?.updateValueAndValidity();
    // this.fg.get('sourceDataConnection')?.updateValueAndValidity();
    // this.fg.get('sourceBucket')?.updateValueAndValidity();
    // this.fg.get('sourceFolder')?.updateValueAndValidity();
    if (type) {
      
    } else {

    }
  }

  public continue(): void {
    // this.router.navigate([`projects/${this.projectId}/data-quality/rule-types`]);
    this.snakbar.open("Rule saved");
    this.router.navigate([`projects/${this.projectId}/data-quality`]);
  }

  public showData(): void {
    this.isShowData = !this.isShowData;
    if (!this.fg.valid) {
      return;
    }

  }

  public back(): void {
    this.router.navigate([`projects/${this.projectId}/data-quality`]);
  }

  public onChangeRuleTypes(value: string): void {
    this.selectedSubLevel = null;
    this.selectedMainRuleType = value;
    
  }

  public onChangeSubLevel(value: string): void {
    this.selectedSubLevel = value;
  }

  // browse table 
  public browseTable(): void {
    this.router.navigate([`projects/${this.projectId}/data-quality/create/table`]);
  }

  public browseSourceOne(s: string): void {
    sessionStorage.setItem("source", s);
    const form_obj = {};
    Object.keys(this.fg.controls).forEach(key => {
      form_obj[key] = this.fg.get(key).value;
    });
    sessionStorage.setItem("formData", JSON.stringify(form_obj));
    sessionStorage.setItem('check', 'false');
    this.router.navigate([`projects/${this.projectId}/data-quality/create/table`]);
  }

  public browseSourceTwo(s: string): void {
    sessionStorage.setItem("source", s);
    const form_obj = {};
    Object.keys(this.fg.controls).forEach(key => {
      form_obj[key] = this.fg.get(key).value;
    });
    sessionStorage.setItem("formData", JSON.stringify(form_obj));
    sessionStorage.setItem('check', 'false');
    this.router.navigate([`projects/${this.projectId}/data-quality/create/table`]);
  }

  // public browseFileSourceOne(s: string): void {
  //   sessionStorage.setItem()
  //   this.router.navigate([`projects/${this.projectId}/data-quality/create/table`]);
  // }

  modelChangeRuleName(str) {
    // const re = /^[a-zA-Z0-9_]+$/;

    if (this.fg.controls.ruleName.value != "") {

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

  modelChangeDescription(str) {
    if(this.fg.controls.ruleDescription.value != ""){
      const validPattern = String(str)
      .match(
        /^[A-Za-z\s!@#$%^&*()\-_+=\[\]{}|;:'",.<>?/]*$/
      );

      if (validPattern == null) {
        this.invalidDesPattern = true;

      } else {
        this.invalidDesPattern = false;
      }
    } else {
      this.invalidDesPattern = false;
    }
  }

  public onSelectDataConnections1(id: string): void {
    this.bucketSourceOne = [];
    // this.fg.controls.sourceBucketOne.setValue('');
    this.fg.controls.sourceFolderPath.setValue('');
    const connectionBucket = this.dataConnectionList.find(e => e.id == +id);
    this.bucketSourceOne.push(connectionBucket.bucketName);
  }

  public onSelectDataConnections2(id: string): void {
    this.bucketSourceTwo = [];
    // this.fg.controls.sourceBucketTwo.setValue('');
    this.fg.controls.sourceFolderPathTwo.setValue('');
    
    const connectionBucket = this.dataConnectionList.find(e => e.id == id);
    this.bucketSourceTwo.push(connectionBucket.bucketName);
  }

  public onSelectBucketOne(d: string): void {

  }

  public onSelectBucketTwo(d: string): void {

  }

  public saveRuleFunction(): void {
    if (this.fg.controls.sourceAndTarget.value) {
      this.saveRulePayload = {
        ruleName:this.fg.controls.ruleName.value,
        ruleDescription:this.fg.controls.ruleDescription.value,
        sourceAndTarget:this.fg.controls.sourceAndTarget.value,
        sourceData : {
            dataSourceType: this.fg.controls.sourceDataSource.value,
            subDataSourceType: this.fg.controls.subDataSourceOne.value,
            dataSourceId: +this.fg.controls.sourceDataConnection.value,
            bucketName: this.fg.controls.sourceBucketOne.value,
            filePath : this.fg.controls.sourceFolderPath.value
        },
        targetData: {
          dataSourceType: this.fg.controls.sourceDataSourceTwo.value,
          subDataSourceType: this.fg.controls.subDataSourceTwo.value,
          dataSourceId: +this.fg.controls.sourceDataConnectionTwo.value,
          bucketName: this.fg.controls.sourceBucketTwo.value,
          filePath : this.fg.controls.sourceFolderPathTwo.value
        },
        ruleDetails:{
            ruleTypeName : this.fg.controls.ruleType.value,
            ruleLevel :{
              levelName: this.fg.controls.subLavelRadio.value,
              columnLevel: false,
              acceptance: this.fg.controls.acceptancePercentage.value,
              columns: [this.fg.controls.selectColumnAttribute.value]
            }
    
        },
        userId : this.userId    
      }
      // console.log("Rule save payload:", JSON.stringify(req));
    } else {
      this.saveRulePayload = {
        ruleName:this.fg.controls.ruleName.value,
        ruleDescription:this.fg.controls.ruleDescription.value,
        sourceAndTarget:this.fg.controls.sourceAndTarget.value,
        sourceData : {
            dataSourceType: this.fg.controls.sourceDataSource.value,
            subDataSourceType: this.fg.controls.subDataSourceOne.value,
            dataSourceId: +this.fg.controls.sourceDataConnection.value,
            bucketName: this.fg.controls.sourceBucketOne.value,
            filePath : this.fg.controls.sourceFolderPath.value
        },
        ruleDetails:{
            ruleTypeName : this.fg.controls.ruleType.value,
            ruleLevel :{
              levelName: this.fg.controls.subLavelRadio.value,
              columnLevel: false,
              acceptance: this.fg.controls.acceptancePercentage.value,
              columns: [this.fg.controls.selectColumnAttribute.value]
            }
    
        },
        userId : this.userId    
      }

      // console.log("FALSE -- Rule save payload:", JSON.stringify(req));
    }

    this.ruleCreationService.saveRule(this.userId, this.projectId, this.saveRulePayload).subscribe((response) => {
      this.snakbar.open(response.message);
      this.router.navigate([`projects/${this.projectId}/data-quality`]);
    }, (error) => {

    });
  }

}